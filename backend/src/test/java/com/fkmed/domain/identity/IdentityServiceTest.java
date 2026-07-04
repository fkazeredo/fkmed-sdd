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
import org.springframework.security.crypto.password.PasswordEncoder;

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

  @Mock private UserAccountRepository accounts;
  @Mock private EmailVerificationTokenRepository verificationTokens;
  @Mock private TermAcceptanceRepository termAcceptances;
  @Mock private Beneficiaries beneficiaries;
  @Mock private PasswordEncoder passwordEncoder;
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
    IdentitySettings settings = new IdentitySettings(Duration.ofHours(24), "1.0", "1.0");
    service =
        new IdentityService(
            accounts,
            verificationTokens,
            termAcceptances,
            beneficiaries,
            passwordPolicy,
            passwordEncoder,
            registrationTokens,
            auditRecorder,
            events,
            settings,
            CLOCK);
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
}
