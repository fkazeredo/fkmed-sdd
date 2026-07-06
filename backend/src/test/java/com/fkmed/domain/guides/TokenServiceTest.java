package com.fkmed.domain.guides;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.AccountStatus;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.BeneficiaryNotAccessibleException;
import com.fkmed.domain.plan.BeneficiaryRole;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * SPEC-0012 BR9/BR12: generating a token invalidates any previous active token for the beneficiary
 * first (single-validity), and a titular generating one for a dependent is audited — never for a
 * self-generation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-06T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final String CARD = "001234567";
  private static final UUID TITULAR = UUID.fromString("b0000000-0000-4000-8000-000000000001");
  private static final UUID DEPENDENT = UUID.fromString("b0000000-0000-4000-8000-000000000002");
  private static final UUID AUTHOR_ACCOUNT =
      UUID.fromString("a0000000-0000-4000-8000-000000000001");

  @Mock private AttendanceTokenRepository tokens;
  @Mock private BeneficiaryAccess beneficiaryAccess;
  @Mock private IdentityAccounts identityAccounts;
  @Mock private AuditRecorder auditRecorder;

  private TokenService service;

  @BeforeEach
  void setUp() {
    service =
        new TokenService(
            tokens,
            beneficiaryAccess,
            identityAccounts,
            auditRecorder,
            new SimpleMeterRegistry(),
            CLOCK);
    when(identityAccounts.findByEmail(any()))
        .thenReturn(
            Optional.of(
                new AccountCredentials(
                    AUTHOR_ACCOUNT,
                    "titular@fkmed.local",
                    "hash",
                    AccountStatus.ACTIVE,
                    TITULAR,
                    false)));
  }

  @Test
  void generate_forSelf_invalidatesNoPreviousToken_whenNoneActive_andDoesNotAudit() {
    accessibleAsSelf();
    when(tokens.findByBeneficiaryIdAndInvalidatedAtIsNull(TITULAR)).thenReturn(Optional.empty());

    TokenView view = service.generate(CARD, "titular@fkmed.local", TITULAR, AuditContext.none());

    assertThat(view.code()).hasSize(6);
    assertThat(view.expiresAt()).isEqualTo(NOW.plusSeconds(600));
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void generate_whenAPreviousActiveTokenExists_invalidatesItFirst() {
    accessibleAsSelf();
    AttendanceToken previous =
        AttendanceToken.generate(TITULAR, "111111", NOW.minusSeconds(60), null);
    when(tokens.findByBeneficiaryIdAndInvalidatedAtIsNull(TITULAR))
        .thenReturn(Optional.of(previous));

    service.generate(CARD, "titular@fkmed.local", TITULAR, AuditContext.none());

    assertThat(previous.getInvalidatedAt()).isEqualTo(NOW);
    // Regression (found building this slice, TokenApiIT#generate_again_invalidatesThePreviousToken
    // _ac5): the invalidation MUST be flushed immediately, not merely saved — Hibernate's default
    // flush ordering executes pending INSERTs before UPDATEs, so a plain save() would let the new
    // token's insert race the previous one's invalidation and trip the partial unique index.
    verify(tokens).saveAndFlush(previous);
  }

  @Test
  void generate_forADependent_recordsTheAuthorshipAudit() {
    accessibleAsTitularActingForDependent();
    when(tokens.findByBeneficiaryIdAndInvalidatedAtIsNull(DEPENDENT)).thenReturn(Optional.empty());

    service.generate(CARD, "titular@fkmed.local", DEPENDENT, AuditContext.none());

    ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder).record(captor.capture());
    assertThat(captor.getValue().authorAccountId()).isEqualTo(AUTHOR_ACCOUNT);
    assertThat(captor.getValue().targetBeneficiaryId()).isEqualTo(DEPENDENT);
  }

  @Test
  void generate_forAnOutOfScopeBeneficiary_throwsWithoutTouchingTokens() {
    when(beneficiaryAccess.accessibleFor(CARD))
        .thenReturn(List.of(new AccessibleBeneficiary(TITULAR, "Maria", BeneficiaryRole.TITULAR)));

    assertThatExceptionOfType(BeneficiaryNotAccessibleException.class)
        .isThrownBy(
            () ->
                service.generate(
                    CARD, "titular@fkmed.local", UUID.randomUUID(), AuditContext.none()));
    verify(tokens, never()).save(any());
  }

  @Test
  void current_whenNoActiveToken_throwsTokenNoneActive() {
    when(beneficiaryAccess.requireAccessible(CARD, TITULAR))
        .thenReturn(new AccessibleBeneficiary(TITULAR, "Maria", BeneficiaryRole.TITULAR));
    when(tokens.findByBeneficiaryIdAndInvalidatedAtIsNull(TITULAR)).thenReturn(Optional.empty());

    assertThatExceptionOfType(TokenNoneActiveException.class)
        .isThrownBy(() -> service.current(CARD, TITULAR));
  }

  @Test
  void current_whenTheStoredTokenIsExpired_throwsTokenNoneActive() {
    when(beneficiaryAccess.requireAccessible(CARD, TITULAR))
        .thenReturn(new AccessibleBeneficiary(TITULAR, "Maria", BeneficiaryRole.TITULAR));
    AttendanceToken expired =
        AttendanceToken.generate(TITULAR, "222222", NOW.minusSeconds(700), null);
    when(tokens.findByBeneficiaryIdAndInvalidatedAtIsNull(TITULAR))
        .thenReturn(Optional.of(expired));

    assertThatExceptionOfType(TokenNoneActiveException.class)
        .isThrownBy(() -> service.current(CARD, TITULAR));
  }

  @Test
  void current_whenAnActiveTokenExists_returnsIt() {
    when(beneficiaryAccess.requireAccessible(CARD, TITULAR))
        .thenReturn(new AccessibleBeneficiary(TITULAR, "Maria", BeneficiaryRole.TITULAR));
    AttendanceToken active =
        AttendanceToken.generate(TITULAR, "333333", NOW.minusSeconds(60), null);
    when(tokens.findByBeneficiaryIdAndInvalidatedAtIsNull(TITULAR)).thenReturn(Optional.of(active));

    TokenView view = service.current(CARD, TITULAR);

    assertThat(view.code()).isEqualTo("333333");
  }

  private void accessibleAsSelf() {
    when(beneficiaryAccess.accessibleFor(CARD))
        .thenReturn(List.of(new AccessibleBeneficiary(TITULAR, "Maria", BeneficiaryRole.TITULAR)));
  }

  private void accessibleAsTitularActingForDependent() {
    when(beneficiaryAccess.accessibleFor(CARD))
        .thenReturn(
            List.of(
                new AccessibleBeneficiary(TITULAR, "Maria", BeneficiaryRole.TITULAR),
                new AccessibleBeneficiary(DEPENDENT, "Pedro", BeneficiaryRole.DEPENDENT)));
  }
}
