package com.fkmed.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.plan.Beneficiaries;
import com.fkmed.domain.plan.BeneficiaryMatch;
import com.fkmed.domain.plan.BeneficiaryRole;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** SPEC-0002 first-access + verification orchestration (branch coverage for the mutation gate). */
@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-04T12:00:00Z"), ZoneOffset.UTC);
  private static final UUID BENEFICIARY = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");
  private static final String CPF = "52998224725";
  private static final String CARD = "001234567";
  private static final LocalDate ADULT_BIRTH = LocalDate.parse("1988-03-12");
  private static final LocalDate MINOR_BIRTH = LocalDate.parse("2012-01-01");
  private static final AuditContext CONTEXT = new AuditContext("203.0.113.7", "JUnit");

  /** Runs each {@code recordFailedLogin} attempt inline (no real DB) — never signals a conflict. */
  private static final PlatformTransactionManager NO_OP_TX =
      new PlatformTransactionManager() {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
          return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {}

        @Override
        public void rollback(TransactionStatus status) {}
      };

  @Mock private UserAccountRepository accounts;
  @Mock private EmailVerificationTokenRepository verificationTokens;
  @Mock private PasswordResetTokenRepository resetTokens;
  @Mock private TermAcceptanceRepository termAcceptances;
  @Mock private Beneficiaries beneficiaries;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private ActiveSessions activeSessions;
  @Mock private AuditRecorder auditRecorder;
  @Mock private ApplicationEventPublisher events;

  private RegistrationTokenService registrationTokens;
  private IdentityService service;

  @BeforeEach
  void setUp() {
    registrationTokens =
        new RegistrationTokenService(
            "secret".getBytes(StandardCharsets.UTF_8), CLOCK, Duration.ofMinutes(30));
    PasswordPolicy passwordPolicy = new PasswordPolicy(password -> false);
    IdentitySettings settings =
        new IdentitySettings(Duration.ofHours(24), Duration.ofMinutes(30), "1.0", "1.0");
    service =
        new IdentityService(
            accounts,
            verificationTokens,
            resetTokens,
            termAcceptances,
            beneficiaries,
            passwordPolicy,
            passwordEncoder,
            registrationTokens,
            activeSessions,
            auditRecorder,
            events,
            settings,
            NO_OP_TX,
            CLOCK);
  }

  private static UserAccount activeAccount(String email) {
    UserAccount account =
        UserAccount.register(BENEFICIARY, email, "{bcrypt}current-hash", CLOCK.instant());
    account.activate();
    return account;
  }

  private void matchReturns(BeneficiaryRole role, LocalDate birthDate) {
    when(beneficiaries.matchForFirstAccess(CPF, CARD, birthDate))
        .thenReturn(Optional.of(new BeneficiaryMatch(BENEFICIARY, CARD, birthDate, role)));
  }

  @Test
  void verify_returnsATokenBoundToTheBeneficiary_forAValidTitular() {
    matchReturns(BeneficiaryRole.TITULAR, ADULT_BIRTH);
    when(accounts.existsByBeneficiaryId(BENEFICIARY)).thenReturn(false);

    String token = service.verifyFirstAccess(CPF, CARD, ADULT_BIRTH);

    assertThat(registrationTokens.verify(token)).isEqualTo(BENEFICIARY);
  }

  @Test
  void verify_refusesAnUnknownTriple() {
    when(beneficiaries.matchForFirstAccess(CPF, CARD, ADULT_BIRTH)).thenReturn(Optional.empty());
    assertThatExceptionOfType(RegistrationNotFoundException.class)
        .isThrownBy(() -> service.verifyFirstAccess(CPF, CARD, ADULT_BIRTH));
  }

  @Test
  void verify_refusesAnInvalidChecksumCpf_withTheSameGenericError_beforeQueryingBeneficiaries() {
    // SPEC-0002 §Validation Rules + BR1 neutrality: a checksum-invalid CPF never reaches the
    // beneficiary lookup — it fails exactly like a real mismatch, with no distinguishable trace.
    String tamperedChecksum = "52998224724"; // same fixture as BeneficiaryTest: wrong 2nd digit
    assertThatExceptionOfType(RegistrationNotFoundException.class)
        .isThrownBy(() -> service.verifyFirstAccess(tamperedChecksum, CARD, ADULT_BIRTH));
    verify(beneficiaries, never()).matchForFirstAccess(anyString(), anyString(), any());
  }

  @Test
  void verify_refusesAnUnderageDependent() {
    matchReturns(BeneficiaryRole.DEPENDENT, MINOR_BIRTH);
    assertThatExceptionOfType(DependentUnderageException.class)
        .isThrownBy(() -> service.verifyFirstAccess(CPF, CARD, MINOR_BIRTH));
  }

  @Test
  void verify_allowsAnAdultDependent() {
    matchReturns(BeneficiaryRole.DEPENDENT, ADULT_BIRTH);
    when(accounts.existsByBeneficiaryId(BENEFICIARY)).thenReturn(false);
    assertThatCode(() -> service.verifyFirstAccess(CPF, CARD, ADULT_BIRTH))
        .doesNotThrowAnyException();
  }

  @Test
  void verify_allowsADependentWhoTurnsEighteenExactlyToday() {
    // CLOCK is fixed at 2026-07-04; born exactly 18 years earlier.
    LocalDate eighteenthBirthdayToday = LocalDate.parse("2008-07-04");
    matchReturns(BeneficiaryRole.DEPENDENT, eighteenthBirthdayToday);
    when(accounts.existsByBeneficiaryId(BENEFICIARY)).thenReturn(false);

    assertThatCode(() -> service.verifyFirstAccess(CPF, CARD, eighteenthBirthdayToday))
        .doesNotThrowAnyException();
  }

  @Test
  void verify_refusesADependentOneDayShortOfEighteen() {
    // CLOCK is fixed at 2026-07-04; their 18th birthday is tomorrow (17 years, 364 days today).
    LocalDate oneDayShortOfEighteen = LocalDate.parse("2008-07-05");
    matchReturns(BeneficiaryRole.DEPENDENT, oneDayShortOfEighteen);

    assertThatExceptionOfType(DependentUnderageException.class)
        .isThrownBy(() -> service.verifyFirstAccess(CPF, CARD, oneDayShortOfEighteen));
  }

  @Test
  void verify_refusesWhenTheBeneficiaryAlreadyHasAnAccount() {
    matchReturns(BeneficiaryRole.TITULAR, ADULT_BIRTH);
    when(accounts.existsByBeneficiaryId(BENEFICIARY)).thenReturn(true);
    assertThatExceptionOfType(AccountAlreadyExistsException.class)
        .isThrownBy(() -> service.verifyFirstAccess(CPF, CARD, ADULT_BIRTH));
  }

  @Test
  void complete_createsAccount_recordsAcceptances_andPublishesAccountCreated() {
    String token = registrationTokens.issue(BENEFICIARY);
    when(accounts.existsByBeneficiaryId(BENEFICIARY)).thenReturn(false);
    when(accounts.existsByEmail("user@fkmed.local")).thenReturn(false);
    when(passwordEncoder.encode("Abcd1234")).thenReturn("{bcrypt}hash");
    when(accounts.save(any(UserAccount.class))).thenAnswer(call -> call.getArgument(0));

    service.completeFirstAccess(token, "User@Fkmed.local", "Abcd1234", CONTEXT);

    ArgumentCaptor<UserAccount> savedAccount = ArgumentCaptor.forClass(UserAccount.class);
    verify(accounts).save(savedAccount.capture());
    UUID accountId = savedAccount.getValue().getId();

    // Regression (QA finding): `save(any())` also matches null — capture and assert the real
    // fields (BR15: account, both current document types, current version, timestamp).
    ArgumentCaptor<TermAcceptance> acceptances = ArgumentCaptor.forClass(TermAcceptance.class);
    verify(termAcceptances, times(2)).save(acceptances.capture());
    List<TermAcceptance> recorded = acceptances.getAllValues();
    assertThat(recorded)
        .extracting(TermAcceptance::getDocumentType)
        .containsExactlyInAnyOrder(
            LegalDocumentTypes.TERMS_OF_USE, LegalDocumentTypes.PRIVACY_POLICY);
    assertThat(recorded)
        .allSatisfy(
            acceptance -> {
              assertThat(acceptance.getAccountId()).isEqualTo(accountId);
              assertThat(acceptance.getVersion()).isEqualTo("1.0");
              assertThat(acceptance.getAcceptedAt()).isEqualTo(CLOCK.instant());
            });

    verify(verificationTokens).save(any(EmailVerificationToken.class));

    ArgumentCaptor<AuditEntry> audit = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder).record(audit.capture());
    assertThat(audit.getValue().eventType()).isEqualTo(AuditEventTypes.ACCOUNT_CREATED);
    assertThat(audit.getValue().targetBeneficiaryId()).isEqualTo(BENEFICIARY);

    ArgumentCaptor<AccountCreated> event = ArgumentCaptor.forClass(AccountCreated.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().beneficiaryId()).isEqualTo(BENEFICIARY);
    assertThat(event.getValue().email()).isEqualTo("user@fkmed.local");
    assertThat(event.getValue().verificationToken()).isNotBlank();
  }

  @Test
  void complete_refusesADuplicateEmail() {
    String token = registrationTokens.issue(BENEFICIARY);
    when(accounts.existsByBeneficiaryId(BENEFICIARY)).thenReturn(false);
    when(accounts.existsByEmail("user@fkmed.local")).thenReturn(true);

    assertThatExceptionOfType(EmailAlreadyUsedException.class)
        .isThrownBy(
            () -> service.completeFirstAccess(token, "user@fkmed.local", "Abcd1234", CONTEXT));
    verify(accounts, never()).save(any());
  }

  @Test
  void complete_refusesWhenTheBeneficiaryAlreadyHasAnAccount() {
    String token = registrationTokens.issue(BENEFICIARY);
    when(accounts.existsByBeneficiaryId(BENEFICIARY)).thenReturn(true);

    assertThatExceptionOfType(AccountAlreadyExistsException.class)
        .isThrownBy(
            () -> service.completeFirstAccess(token, "user@fkmed.local", "Abcd1234", CONTEXT));
  }

  @Test
  void complete_refusesAnInvalidRegistrationToken() {
    assertThatExceptionOfType(RegistrationNotFoundException.class)
        .isThrownBy(
            () -> service.completeFirstAccess("garbage", "user@fkmed.local", "Abcd1234", CONTEXT));
  }

  @Test
  void confirm_activatesTheAccount_andAuditsEmailVerified() {
    UserAccount account =
        UserAccount.register(BENEFICIARY, "user@fkmed.local", "{bcrypt}hash", CLOCK.instant());
    String hash = SecureTokens.sha256Hex("raw-token");
    EmailVerificationToken token =
        EmailVerificationToken.issue(account.getId(), hash, CLOCK.instant(), Duration.ofHours(24));
    when(verificationTokens.findByTokenHash(hash)).thenReturn(Optional.of(token));
    when(accounts.findById(account.getId())).thenReturn(Optional.of(account));

    service.confirmVerification("raw-token", CONTEXT);

    assertThat(account.isActive()).isTrue();
    assertThat(token.getUsedAt()).isNotNull();
    ArgumentCaptor<AuditEntry> audit = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder).record(audit.capture());
    assertThat(audit.getValue().eventType()).isEqualTo(AuditEventTypes.EMAIL_VERIFIED);
  }

  @Test
  void confirm_rejectsAnUnknownToken() {
    when(verificationTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());
    assertThatExceptionOfType(VerificationLinkInvalidException.class)
        .isThrownBy(() -> service.confirmVerification("raw-token", CONTEXT));
  }

  @Test
  void confirm_rejectsAnAlreadyUsedToken() {
    // Regression (QA/PIT finding): accounts.findById was left unstubbed, so an unrelated
    // Optional.empty() default was the ACTUAL reason the test passed — isUsable(now) being
    // mutated to always-true went undetected. Stubbing it here (lenient: the correct code path
    // never reaches it) forces the test to fail unless the "already used" check itself
    // short-circuits before ever reaching findById.
    UserAccount account =
        UserAccount.register(BENEFICIARY, "user@fkmed.local", "{bcrypt}hash", CLOCK.instant());
    String hash = SecureTokens.sha256Hex("raw-token");
    EmailVerificationToken token =
        EmailVerificationToken.issue(account.getId(), hash, CLOCK.instant(), Duration.ofHours(24));
    token.markUsed(CLOCK.instant());
    when(verificationTokens.findByTokenHash(hash)).thenReturn(Optional.of(token));
    lenient().when(accounts.findById(account.getId())).thenReturn(Optional.of(account));

    assertThatExceptionOfType(VerificationLinkInvalidException.class)
        .isThrownBy(() -> service.confirmVerification("raw-token", CONTEXT));

    assertThat(account.isActive()).isFalse();
    verify(accounts, never()).findById(any());
  }

  @Test
  void resend_isNeutralWhenNoAccountExists() {
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.empty());
    service.resendVerification("user@fkmed.local");
    verify(events, never()).publishEvent(any());
    verify(verificationTokens, never()).save(any());
  }

  @Test
  void resend_reissuesForAnUnverifiedAccount() {
    UserAccount account =
        UserAccount.register(BENEFICIARY, "user@fkmed.local", "{bcrypt}hash", CLOCK.instant());
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.of(account));

    service.resendVerification("user@fkmed.local");

    verify(verificationTokens).invalidateOpenTokens(eq(account.getId()), any());
    verify(verificationTokens).save(any(EmailVerificationToken.class));
    verify(events).publishEvent(any(AccountCreated.class));
  }

  @Test
  void resend_isNoOpForAnAlreadyActiveAccount() {
    UserAccount account =
        UserAccount.register(BENEFICIARY, "user@fkmed.local", "{bcrypt}hash", CLOCK.instant());
    account.activate();
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.of(account));

    service.resendVerification("user@fkmed.local");

    verify(events, never()).publishEvent(any());
  }

  // ---- SPEC-0002 BR8 lockout accounting (DL-0002) ----

  @Test
  void recordFailedLogin_locksAndAuditsOnce_onTheFifthConsecutiveFailure() {
    UserAccount account = activeAccount("user@fkmed.local");
    for (int i = 0; i < 4; i++) {
      account.registerFailedLogin(CLOCK.instant());
    }
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.of(account));

    service.recordFailedLogin("user@fkmed.local", CONTEXT);

    assertThat(account.isLocked(CLOCK.instant())).isTrue();
    ArgumentCaptor<AuditEntry> audit = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder).record(audit.capture());
    assertThat(audit.getValue().eventType()).isEqualTo(AuditEventTypes.ACCOUNT_LOCKED);
    assertThat(audit.getValue().targetBeneficiaryId()).isEqualTo(BENEFICIARY);
  }

  @Test
  void recordFailedLogin_belowThreshold_incrementsWithoutLockingOrAuditing() {
    UserAccount account = activeAccount("user@fkmed.local");
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.of(account));

    service.recordFailedLogin("user@fkmed.local", CONTEXT);

    assertThat(account.getFailedAttempts()).isEqualTo(1);
    assertThat(account.isLocked(CLOCK.instant())).isFalse();
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void recordFailedLogin_whileAlreadyLocked_isANoOp_andDoesNotReAudit() {
    UserAccount account = activeAccount("user@fkmed.local");
    for (int i = 0; i < 5; i++) {
      account.registerFailedLogin(CLOCK.instant());
    }
    assertThat(account.isLocked(CLOCK.instant())).isTrue();
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.of(account));

    service.recordFailedLogin("user@fkmed.local", CONTEXT);

    // The 15-minute window is measured from the 5th failure, never extended by later attempts.
    assertThat(account.getFailedAttempts()).isEqualTo(5);
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void recordFailedLogin_nonexistentEmail_touchesNoRow_forBr7Neutrality() {
    when(accounts.findByEmail("ghost@fkmed.local")).thenReturn(Optional.empty());

    service.recordFailedLogin("ghost@fkmed.local", CONTEXT);

    verify(auditRecorder, never()).record(any());
  }

  @Test
  void recordFailedLogin_whenTheConflictNeverReconciles_translatesToTheDomainError() {
    // Débito técnico A (DL-0005): an unrelenting optimistic-lock conflict must surface as the
    // domain ConcurrentAccountUpdateException (→ 409), NEVER the raw framework exception.
    PlatformTransactionManager alwaysConflicts =
        new PlatformTransactionManager() {
          @Override
          public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
          }

          @Override
          public void commit(TransactionStatus status) {
            throw new ObjectOptimisticLockingFailureException(UserAccount.class, UUID.randomUUID());
          }

          @Override
          public void rollback(TransactionStatus status) {}
        };
    IdentityService conflicting =
        new IdentityService(
            accounts,
            verificationTokens,
            resetTokens,
            termAcceptances,
            beneficiaries,
            new PasswordPolicy(password -> false),
            passwordEncoder,
            registrationTokens,
            activeSessions,
            auditRecorder,
            events,
            new IdentitySettings(Duration.ofHours(24), Duration.ofMinutes(30), "1.0", "1.0"),
            alwaysConflicts,
            CLOCK);

    assertThatExceptionOfType(ConcurrentAccountUpdateException.class)
        .isThrownBy(() -> conflicting.recordFailedLogin("user@fkmed.local", CONTEXT));
  }

  @Test
  void recordSuccessfulLogin_clearsTheFailureCounter() {
    UserAccount account = activeAccount("user@fkmed.local");
    for (int i = 0; i < 3; i++) {
      account.registerFailedLogin(CLOCK.instant());
    }
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.of(account));

    service.recordSuccessfulLogin("user@fkmed.local");

    assertThat(account.getFailedAttempts()).isZero();
    assertThat(account.getLockedUntil()).isNull();
  }

  // ---- SPEC-0002 BR10 recovery request (DL-0003) ----

  @Test
  void requestPasswordRecovery_forAnActiveAccount_issuesASingleUseLink_auditsAndPublishes() {
    UserAccount account = activeAccount("user@fkmed.local");
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.of(account));

    service.requestPasswordRecovery("user@fkmed.local", CONTEXT);

    verify(resetTokens).invalidateOpenTokens(eq(account.getId()), any());
    verify(resetTokens).save(any(PasswordResetToken.class));
    ArgumentCaptor<AuditEntry> audit = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder).record(audit.capture());
    assertThat(audit.getValue().eventType()).isEqualTo(AuditEventTypes.PASSWORD_RECOVERY_REQUESTED);
    ArgumentCaptor<PasswordRecoveryRequested> event =
        ArgumentCaptor.forClass(PasswordRecoveryRequested.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().email()).isEqualTo("user@fkmed.local");
    assertThat(event.getValue().resetToken()).isNotBlank();
  }

  @Test
  void requestPasswordRecovery_forAnUnverifiedAccount_isASilentNoOp() {
    UserAccount account =
        UserAccount.register(BENEFICIARY, "user@fkmed.local", "{bcrypt}hash", CLOCK.instant());
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.of(account));

    service.requestPasswordRecovery("user@fkmed.local", CONTEXT);

    verify(resetTokens, never()).save(any());
    verify(events, never()).publishEvent(any());
  }

  @Test
  void requestPasswordRecovery_forANonexistentEmail_isASilentNoOp() {
    when(accounts.findByEmail("ghost@fkmed.local")).thenReturn(Optional.empty());

    service.requestPasswordRecovery("ghost@fkmed.local", CONTEXT);

    verify(resetTokens, never()).save(any());
    verify(events, never()).publishEvent(any());
  }

  // ---- SPEC-0002 BR10 reset (DL-0003) ----

  @Test
  void resetPassword_withAValidLink_setsThePassword_consumesTheLink_terminatesSessions() {
    UserAccount account = activeAccount("user@fkmed.local");
    String rawToken = "raw-reset-token";
    String hash = SecureTokens.sha256Hex(rawToken);
    PasswordResetToken token =
        PasswordResetToken.issue(account.getId(), hash, CLOCK.instant(), Duration.ofMinutes(30));
    when(resetTokens.findByTokenHash(hash)).thenReturn(Optional.of(token));
    when(accounts.findById(account.getId())).thenReturn(Optional.of(account));
    when(passwordEncoder.encode("BrandNew123")).thenReturn("{bcrypt}new-hash");
    when(activeSessions.terminateAllFor("user@fkmed.local")).thenReturn(2);

    service.resetPassword(rawToken, "BrandNew123", CONTEXT);

    assertThat(account.getPasswordHash()).isEqualTo("{bcrypt}new-hash");
    assertThat(token.getUsedAt()).isNotNull();
    verify(activeSessions).terminateAllFor("user@fkmed.local");
    ArgumentCaptor<AuditEntry> audit = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder).record(audit.capture());
    assertThat(audit.getValue().eventType()).isEqualTo(AuditEventTypes.PASSWORD_CHANGED);
    assertThat(audit.getValue().details())
        .containsEntry("flow", PasswordChanged.RECOVERY_RESET)
        .containsEntry("sessionsTerminated", "2");
    ArgumentCaptor<PasswordChanged> event = ArgumentCaptor.forClass(PasswordChanged.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().flow()).isEqualTo(PasswordChanged.RECOVERY_RESET);
  }

  @Test
  void resetPassword_withAPolicyViolatingPassword_isRefused_withoutTouchingSessions() {
    UserAccount account = activeAccount("user@fkmed.local");
    String rawToken = "raw-reset-token";
    String hash = SecureTokens.sha256Hex(rawToken);
    PasswordResetToken token =
        PasswordResetToken.issue(account.getId(), hash, CLOCK.instant(), Duration.ofMinutes(30));
    when(resetTokens.findByTokenHash(hash)).thenReturn(Optional.of(token));
    when(accounts.findById(account.getId())).thenReturn(Optional.of(account));

    // BR9 applies to the reset too (DL-0003): the base policy is enforced before anything changes.
    assertThatExceptionOfType(PasswordPolicyViolationException.class)
        .isThrownBy(() -> service.resetPassword(rawToken, "short", CONTEXT));
    assertThat(token.getUsedAt()).isNull();
    verify(activeSessions, never()).terminateAllFor(any());
    verify(events, never()).publishEvent(any());
  }

  @Test
  void resetPassword_withAnUnknownToken_isRejected() {
    when(resetTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResetLinkInvalidException.class)
        .isThrownBy(() -> service.resetPassword("nope", "BrandNew123", CONTEXT));
    verify(events, never()).publishEvent(any());
  }

  @Test
  void resetPassword_withAnAlreadyUsedToken_isRejected_beforeReachingTheAccount() {
    String rawToken = "raw-reset-token";
    String hash = SecureTokens.sha256Hex(rawToken);
    PasswordResetToken token =
        PasswordResetToken.issue(UUID.randomUUID(), hash, CLOCK.instant(), Duration.ofMinutes(30));
    token.markUsed(CLOCK.instant());
    when(resetTokens.findByTokenHash(hash)).thenReturn(Optional.of(token));

    assertThatExceptionOfType(ResetLinkInvalidException.class)
        .isThrownBy(() -> service.resetPassword(rawToken, "BrandNew123", CONTEXT));
    // The "already used" filter short-circuits: the account is never loaded, nothing is terminated.
    verify(accounts, never()).findById(any());
    verify(activeSessions, never()).terminateAllFor(any());
  }

  // ---- SPEC-0002 BR11 authenticated change (DL-0003) ----

  @Test
  void changePassword_withTheCorrectCurrent_setsANewPassword_auditsSelfChange_keepsSessions() {
    UserAccount account = activeAccount("user@fkmed.local");
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.of(account));
    when(passwordEncoder.matches("current", account.getPasswordHash())).thenReturn(true);
    when(passwordEncoder.matches("BrandNew123", account.getPasswordHash())).thenReturn(false);
    when(passwordEncoder.encode("BrandNew123")).thenReturn("{bcrypt}new-hash");

    service.changePassword("user@fkmed.local", "current", "BrandNew123", CONTEXT);

    assertThat(account.getPasswordHash()).isEqualTo("{bcrypt}new-hash");
    // BR10/BR11 division (DL-0003): the self-change does NOT mass-terminate sessions.
    verify(activeSessions, never()).terminateAllFor(any());
    ArgumentCaptor<AuditEntry> audit = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder).record(audit.capture());
    assertThat(audit.getValue().details())
        .containsEntry("flow", PasswordChanged.SELF_CHANGE)
        .containsEntry("sessionsTerminated", "0");
    ArgumentCaptor<PasswordChanged> event = ArgumentCaptor.forClass(PasswordChanged.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().flow()).isEqualTo(PasswordChanged.SELF_CHANGE);
  }

  @Test
  void changePassword_forAnUnknownEmail_isCurrentPasswordIncorrect() {
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.empty());

    assertThatExceptionOfType(CurrentPasswordIncorrectException.class)
        .isThrownBy(
            () -> service.changePassword("user@fkmed.local", "current", "BrandNew123", CONTEXT));
    verify(events, never()).publishEvent(any());
  }

  @Test
  void changePassword_withAWrongCurrentPassword_isCurrentPasswordIncorrect() {
    UserAccount account = activeAccount("user@fkmed.local");
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.of(account));
    when(passwordEncoder.matches("wrong", account.getPasswordHash())).thenReturn(false);

    assertThatExceptionOfType(CurrentPasswordIncorrectException.class)
        .isThrownBy(
            () -> service.changePassword("user@fkmed.local", "wrong", "BrandNew123", CONTEXT));
    verify(events, never()).publishEvent(any());
  }

  @Test
  void changePassword_whenTheNewPasswordEqualsTheCurrent_isAPolicyViolation() {
    UserAccount account = activeAccount("user@fkmed.local");
    when(accounts.findByEmail("user@fkmed.local")).thenReturn(Optional.of(account));
    // Correct current password AND the differ-from-current check both match the stored hash.
    when(passwordEncoder.matches("SamePass123", account.getPasswordHash())).thenReturn(true);

    assertThatExceptionOfType(PasswordPolicyViolationException.class)
        .isThrownBy(
            () ->
                service.changePassword("user@fkmed.local", "SamePass123", "SamePass123", CONTEXT));
    verify(events, never()).publishEvent(any());
  }
}
