package com.fkmed.domain.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
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
import java.time.ZoneId;
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
 * SPEC-0014 BR4: registering a Libras service request scope-checks the beneficiary, persists it as
 * {@code REGISTERED}, always audits the registration and reports the correct next step depending on
 * whether "now" falls inside the operating window.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LibrasRequestsTest {

  private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  // Wednesday 2026-07-08, 10:00 — inside the 8h-18h weekday window.
  private static final Instant WITHIN_HOURS =
      java.time.ZonedDateTime.of(2026, 7, 8, 10, 0, 0, 0, ZONE).toInstant();
  // Same Wednesday, 20:00 — outside the window.
  private static final Instant OUTSIDE_HOURS =
      java.time.ZonedDateTime.of(2026, 7, 8, 20, 0, 0, 0, ZONE).toInstant();

  private static final String CARD = "001234567";
  private static final UUID BENEFICIARY = UUID.fromString("b0000000-0000-4000-8000-000000000001");
  private static final UUID AUTHOR_ACCOUNT =
      UUID.fromString("a0000000-0000-4000-8000-000000000001");

  @Mock private LibrasRequestRepository requests;
  @Mock private BeneficiaryAccess beneficiaryAccess;
  @Mock private IdentityAccounts identityAccounts;
  @Mock private AuditRecorder auditRecorder;

  private LibrasRequests service;

  @BeforeEach
  void setUp() {
    when(beneficiaryAccess.requireAccessible(CARD, BENEFICIARY))
        .thenReturn(new AccessibleBeneficiary(BENEFICIARY, "Maria", BeneficiaryRole.TITULAR));
    when(identityAccounts.findByEmail(any()))
        .thenReturn(
            Optional.of(
                new AccountCredentials(
                    AUTHOR_ACCOUNT,
                    "maria@fkmed.local",
                    "hash",
                    AccountStatus.ACTIVE,
                    BENEFICIARY,
                    false)));
  }

  @Test
  void register_withinOperatingHours_returnsVideocallShortlyWithNoHours() {
    service = newService(Clock.fixed(WITHIN_HOURS, ZONE));

    LibrasRequestResponse response =
        service.register(CARD, "maria@fkmed.local", BENEFICIARY, AuditContext.none());

    assertThat(response.situation()).isEqualTo(LibrasSituation.REGISTERED);
    assertThat(response.nextStep()).isEqualTo("videocall-shortly");
    assertThat(response.hours()).isNull();
  }

  @Test
  void register_outsideOperatingHours_returnsNextPeriodWithHours() {
    service = newService(Clock.fixed(OUTSIDE_HOURS, ZONE));

    LibrasRequestResponse response =
        service.register(CARD, "maria@fkmed.local", BENEFICIARY, AuditContext.none());

    assertThat(response.nextStep()).isEqualTo("next-period");
    assertThat(response.hours()).isEqualTo("Segunda a sexta, das 8h às 18h");
  }

  @Test
  void register_persistsTheRequestAsRegistered() {
    service = newService(Clock.fixed(WITHIN_HOURS, ZONE));

    service.register(CARD, "maria@fkmed.local", BENEFICIARY, AuditContext.none());

    ArgumentCaptor<LibrasRequest> captor = ArgumentCaptor.forClass(LibrasRequest.class);
    verify(requests).save(captor.capture());
    assertThat(captor.getValue().getBeneficiaryId()).isEqualTo(BENEFICIARY);
    assertThat(captor.getValue().getSituation()).isEqualTo(LibrasSituation.REGISTERED);
  }

  @Test
  void register_alwaysAuditsTheRegistration() {
    service = newService(Clock.fixed(WITHIN_HOURS, ZONE));

    service.register(CARD, "maria@fkmed.local", BENEFICIARY, AuditContext.none());

    ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder).record(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo(AuditEventTypes.LIBRAS_REQUEST_REGISTERED);
    assertThat(captor.getValue().authorAccountId()).isEqualTo(AUTHOR_ACCOUNT);
    assertThat(captor.getValue().targetBeneficiaryId()).isEqualTo(BENEFICIARY);
  }

  @Test
  void register_forAnOutOfScopeBeneficiary_throwsWithoutPersistingOrAuditing() {
    service = newService(Clock.fixed(WITHIN_HOURS, ZONE));
    UUID outOfScope = UUID.randomUUID();
    when(beneficiaryAccess.requireAccessible(CARD, outOfScope))
        .thenThrow(new BeneficiaryNotAccessibleException());

    assertThatExceptionOfType(BeneficiaryNotAccessibleException.class)
        .isThrownBy(
            () -> service.register(CARD, "maria@fkmed.local", outOfScope, AuditContext.none()));

    verify(requests, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  private LibrasRequests newService(Clock clock) {
    return new LibrasRequests(
        requests,
        beneficiaryAccess,
        identityAccounts,
        auditRecorder,
        new SimpleMeterRegistry(),
        clock);
  }
}
