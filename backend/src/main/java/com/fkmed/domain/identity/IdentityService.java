package com.fkmed.domain.identity;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.audit.Masking;
import com.fkmed.domain.plan.Beneficiaries;
import com.fkmed.domain.plan.BeneficiaryMatch;
import com.fkmed.domain.plan.BeneficiaryRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service of the identity module (SPEC-0002 first access + e-mail verification). Each
 * public method is a use case; domain rules live in the entities, {@link PasswordPolicy} and {@link
 * RegistrationTokenService}. Audit entries are written in the same transaction as the action they
 * record (SPEC-0003 BR4/BR6); the verification e-mail is delivered off-transaction by an infra
 * listener on {@link AccountCreated} (AFTER_COMMIT).
 */
@Service
@RequiredArgsConstructor
public class IdentityService {

  private static final int ADULT_AGE = 18;

  private final UserAccountRepository accounts;
  private final EmailVerificationTokenRepository verificationTokens;
  private final PasswordResetTokenRepository resetTokens;
  private final TermAcceptanceRepository termAcceptances;
  private final Beneficiaries beneficiaries;
  private final PasswordPolicy passwordPolicy;
  private final PasswordEncoder passwordEncoder;
  private final RegistrationTokenService registrationTokens;
  private final ActiveSessions activeSessions;
  private final AuditRecorder auditRecorder;
  private final ApplicationEventPublisher events;
  private final IdentitySettings settings;
  private final Clock clock;

  /**
   * Validates the identity triple and returns a registration token (SPEC-0002 BR1). Refuses a
   * dependent under 18 (BR3) and a beneficiary that already has an account (BR2). Any triple
   * mismatch is a single generic refusal.
   *
   * @throws RegistrationNotFoundException when the triple matches no active beneficiary.
   * @throws DependentUnderageException when the beneficiary is a dependent younger than 18.
   * @throws AccountAlreadyExistsException when the beneficiary already has an account.
   */
  @Transactional(readOnly = true)
  public String verifyFirstAccess(String cpf, String cardNumber, LocalDate birthDate) {
    // SPEC-0002 §Validation Rules ("CPF: 11 digits, valid check digits") + BR1 neutrality: an
    // invalid checksum is refused with the SAME generic error as a real triple mismatch — it must
    // never surface as a distinct "invalid CPF" failure, or it becomes an oracle for the format
    // check alone. No beneficiary in the seed base can have an invalid-checksum CPF, so this
    // short-circuit changes no legitimate outcome.
    if (!CpfCheckDigits.isValid(cpf)) {
      throw new RegistrationNotFoundException();
    }
    BeneficiaryMatch match =
        beneficiaries
            .matchForFirstAccess(cpf, cardNumber, birthDate)
            .orElseThrow(RegistrationNotFoundException::new);
    if (match.role() == BeneficiaryRole.DEPENDENT && isUnderage(match.birthDate())) {
      throw new DependentUnderageException();
    }
    if (accounts.existsByBeneficiaryId(match.id())) {
      throw new AccountAlreadyExistsException();
    }
    return registrationTokens.issue(match.id());
  }

  /**
   * Creates the account from a registration token: unique e-mail (BR4), password policy (BR9), term
   * acceptances (BR15), a 24h verification link (BR5) and an {@link AccountCreated} event. The
   * account starts {@link AccountStatus#EMAIL_NOT_VERIFIED}.
   *
   * @throws RegistrationNotFoundException when the registration token is invalid/expired.
   * @throws AccountAlreadyExistsException when the beneficiary already has an account (race).
   * @throws EmailAlreadyUsedException when the e-mail is already taken.
   * @throws PasswordPolicyViolationException when the password violates the policy.
   */
  @Transactional
  public void completeFirstAccess(
      String registrationToken, String email, String password, AuditContext auditContext) {
    UUID beneficiaryId = registrationTokens.verify(registrationToken);
    if (accounts.existsByBeneficiaryId(beneficiaryId)) {
      throw new AccountAlreadyExistsException();
    }
    String normalizedEmail = Emails.normalize(email);
    if (accounts.existsByEmail(normalizedEmail)) {
      throw new EmailAlreadyUsedException();
    }
    passwordPolicy.validate(normalizedEmail, password);

    Instant now = clock.instant();
    UserAccount account =
        accounts.save(
            UserAccount.register(
                beneficiaryId, normalizedEmail, passwordEncoder.encode(password), now));
    recordCurrentTermAcceptances(account.getId(), now);

    String rawToken = SecureTokens.newRawToken();
    verificationTokens.save(
        EmailVerificationToken.issue(
            account.getId(),
            SecureTokens.sha256Hex(rawToken),
            now,
            settings.verificationTokenTtl()));

    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.ACCOUNT_CREATED,
            account.getId(),
            beneficiaryId,
            Map.of("email", Masking.email(normalizedEmail)),
            auditContext));
    events.publishEvent(
        new AccountCreated(account.getId(), normalizedEmail, beneficiaryId, rawToken));
  }

  /**
   * Activates the account behind a valid verification link (SPEC-0002 BR5).
   *
   * @throws VerificationLinkInvalidException when the link is unknown, expired or already used.
   */
  @Transactional
  public void confirmVerification(String rawToken, AuditContext auditContext) {
    Instant now = clock.instant();
    EmailVerificationToken token =
        verificationTokens
            .findByTokenHash(SecureTokens.sha256Hex(rawToken))
            .filter(candidate -> candidate.isUsable(now))
            .orElseThrow(VerificationLinkInvalidException::new);
    token.markUsed(now);
    UserAccount account =
        accounts.findById(token.getAccountId()).orElseThrow(VerificationLinkInvalidException::new);
    account.activate();
    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.EMAIL_VERIFIED,
            account.getId(),
            account.getBeneficiaryId(),
            Map.of(),
            auditContext));
  }

  /**
   * Reissues a verification link, invalidating the previous one (SPEC-0002 BR5). Neutral by design
   * (DL-0001): always succeeds silently, sending a fresh link only for an existing,
   * still-unverified account — never revealing whether the e-mail is registered.
   */
  @Transactional
  public void resendVerification(String email) {
    accounts
        .findByEmail(Emails.normalize(email))
        .filter(account -> !account.isActive())
        .ifPresent(this::reissueVerification);
  }

  /**
   * Records a failed login for lockout accounting (SPEC-0002 BR8). No-op for a non-existent e-mail
   * — no row is created or touched, keeping the failure indistinguishable from a wrong password on
   * a real account (BR7). The 5th consecutive failure locks the account for 15 minutes and audits
   * it once; attempts made while already locked are ignored (DL-0002).
   */
  @Transactional
  public void recordFailedLogin(String email, AuditContext auditContext) {
    accounts
        .findByEmail(Emails.normalize(email))
        .ifPresent(account -> lockIfThresholdReached(account, auditContext));
  }

  private void lockIfThresholdReached(UserAccount account, AuditContext auditContext) {
    Instant now = clock.instant();
    boolean wasLocked = account.isLocked(now);
    account.registerFailedLogin(now);
    if (!wasLocked && account.isLocked(now)) {
      auditRecorder.record(
          new AuditEntry(
              AuditEventTypes.ACCOUNT_LOCKED,
              account.getId(),
              account.getBeneficiaryId(),
              Map.of("email", Masking.email(account.getEmail())),
              auditContext));
    }
  }

  /** Resets the failure counter and clears any lock after a successful login (SPEC-0002 BR8). */
  @Transactional
  public void recordSuccessfulLogin(String email) {
    accounts.findByEmail(Emails.normalize(email)).ifPresent(UserAccount::registerSuccessfulLogin);
  }

  /**
   * Requests a password recovery link (SPEC-0002 BR10). Neutral by design (BR7, DL-0003): always
   * succeeds silently, issuing a fresh 30-minute single-use link and auditing the request only for
   * an existing ACTIVE account — never revealing whether the e-mail is registered. A prior open
   * link is invalidated.
   */
  @Transactional
  public void requestPasswordRecovery(String email, AuditContext auditContext) {
    accounts
        .findByEmail(Emails.normalize(email))
        .filter(UserAccount::isActive)
        .ifPresent(account -> issuePasswordReset(account, auditContext));
  }

  private void issuePasswordReset(UserAccount account, AuditContext auditContext) {
    Instant now = clock.instant();
    resetTokens.invalidateOpenTokens(account.getId(), now);
    String rawToken = SecureTokens.newRawToken();
    resetTokens.save(
        PasswordResetToken.issue(
            account.getId(),
            SecureTokens.sha256Hex(rawToken),
            now,
            settings.passwordResetTokenTtl()));
    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.PASSWORD_RECOVERY_REQUESTED,
            account.getId(),
            account.getBeneficiaryId(),
            Map.of("email", Masking.email(account.getEmail())),
            auditContext));
    events.publishEvent(
        new PasswordRecoveryRequested(
            account.getId(), account.getEmail(), account.getBeneficiaryId(), rawToken));
  }

  /**
   * Resets a password behind a valid reset link (SPEC-0002 BR10): validates the base password
   * policy, sets the new password, consumes the single-use link and terminates ALL active sessions
   * of the user. Publishes {@link PasswordChanged} for the security notice.
   *
   * @throws ResetLinkInvalidException when the link is unknown, expired or already used.
   * @throws PasswordPolicyViolationException when the new password violates the policy.
   */
  @Transactional
  public void resetPassword(String rawToken, String newPassword, AuditContext auditContext) {
    Instant now = clock.instant();
    PasswordResetToken token =
        resetTokens
            .findByTokenHash(SecureTokens.sha256Hex(rawToken))
            .filter(candidate -> candidate.isUsable(now))
            .orElseThrow(ResetLinkInvalidException::new);
    UserAccount account =
        accounts.findById(token.getAccountId()).orElseThrow(ResetLinkInvalidException::new);
    passwordPolicy.validate(account.getEmail(), newPassword);
    account.changePassword(passwordEncoder.encode(newPassword));
    token.markUsed(now);
    int terminated = activeSessions.terminateAllFor(account.getEmail());
    recordPasswordChanged(account, PasswordChanged.RECOVERY_RESET, terminated, auditContext);
    events.publishEvent(
        new PasswordChanged(
            account.getId(),
            account.getEmail(),
            account.getBeneficiaryId(),
            PasswordChanged.RECOVERY_RESET));
  }

  /**
   * Changes the password of the authenticated user (SPEC-0002 BR11): requires the correct current
   * password and enforces the full policy including "differ from the current one" (BR9). Does NOT
   * terminate other sessions — that mass-invalidation is specific to the recovery reset (BR10,
   * DL-0003). Publishes {@link PasswordChanged} for the security notice.
   *
   * @throws CurrentPasswordIncorrectException when the current password does not match.
   * @throws PasswordPolicyViolationException when the new password violates the policy or equals
   *     the current one.
   */
  @Transactional
  public void changePassword(
      String email, String currentPassword, String newPassword, AuditContext auditContext) {
    UserAccount account =
        accounts
            .findByEmail(Emails.normalize(email))
            .orElseThrow(CurrentPasswordIncorrectException::new);
    if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
      throw new CurrentPasswordIncorrectException();
    }
    passwordPolicy.validateChange(
        account.getEmail(), newPassword, account.getPasswordHash(), passwordEncoder);
    account.changePassword(passwordEncoder.encode(newPassword));
    recordPasswordChanged(account, PasswordChanged.SELF_CHANGE, 0, auditContext);
    events.publishEvent(
        new PasswordChanged(
            account.getId(),
            account.getEmail(),
            account.getBeneficiaryId(),
            PasswordChanged.SELF_CHANGE));
  }

  private void recordPasswordChanged(
      UserAccount account, String flow, int sessionsTerminated, AuditContext auditContext) {
    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.PASSWORD_CHANGED,
            account.getId(),
            account.getBeneficiaryId(),
            Map.of(
                "email",
                Masking.email(account.getEmail()),
                "flow",
                flow,
                "sessionsTerminated",
                String.valueOf(sessionsTerminated)),
            auditContext));
  }

  private void reissueVerification(UserAccount account) {
    Instant now = clock.instant();
    verificationTokens.invalidateOpenTokens(account.getId(), now);
    String rawToken = SecureTokens.newRawToken();
    verificationTokens.save(
        EmailVerificationToken.issue(
            account.getId(),
            SecureTokens.sha256Hex(rawToken),
            now,
            settings.verificationTokenTtl()));
    // Republishing AccountCreated is intentional reuse of the one existing consumer (the
    // verification-e-mail listener, ADR-0004) — correct today since it is the only subscriber.
    // TRIPWIRE: if a second AccountCreated consumer ever appears (e.g. an analytics/welcome-flow
    // listener in SPEC-0004), it would misfire on every resend too. Revisit with a dedicated
    // event (e.g. VerificationLinkReissued) when that happens — not needed yet (Rule Zero).
    events.publishEvent(
        new AccountCreated(
            account.getId(), account.getEmail(), account.getBeneficiaryId(), rawToken));
  }

  private void recordCurrentTermAcceptances(UUID accountId, Instant now) {
    termAcceptances.save(
        TermAcceptance.record(
            accountId, LegalDocumentTypes.TERMS_OF_USE, settings.termsVersion(), now));
    termAcceptances.save(
        TermAcceptance.record(
            accountId, LegalDocumentTypes.PRIVACY_POLICY, settings.privacyVersion(), now));
  }

  private boolean isUnderage(LocalDate birthDate) {
    return Period.between(birthDate, LocalDate.now(clock)).getYears() < ADULT_AGE;
  }
}
