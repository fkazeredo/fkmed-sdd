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
  private final TermAcceptanceRepository termAcceptances;
  private final Beneficiaries beneficiaries;
  private final PasswordPolicy passwordPolicy;
  private final PasswordEncoder passwordEncoder;
  private final RegistrationTokenService registrationTokens;
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
