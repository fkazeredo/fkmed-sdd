package com.fkmed.domain.support;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.plan.BeneficiaryAccess;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service and public facade for the Central de Libras request (SPEC-0014 BR4): scope
 * check via {@code domain.plan.BeneficiaryAccess} (SPEC-0003 BR2/BR3), unconditional audit of the
 * registration (SPEC-0003 BR6, SPEC-0014 §Observability) and a counter — mirrors the shape of
 * {@code domain.guides.TokenService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LibrasRequests {

  private final LibrasRequestRepository requests;
  private final BeneficiaryAccess beneficiaryAccess;
  private final IdentityAccounts identityAccounts;
  private final AuditRecorder auditRecorder;
  private final MeterRegistry metrics;
  private final Clock clock;

  /**
   * Registers a Libras service request for {@code beneficiaryId} (BR4): persists it as {@code
   * REGISTERED}, audits the registration and reports whether the beneficiary should expect the
   * videocall shortly (inside the operating window) or in the next period (outside it, with the
   * hours).
   *
   * @throws com.fkmed.domain.plan.BeneficiaryNotAccessibleException when the beneficiary is out of
   *     the caller's scope.
   */
  @Transactional
  public LibrasRequestResponse register(
      String callerCard, String authorEmail, UUID beneficiaryId, AuditContext auditContext) {
    beneficiaryAccess.requireAccessible(callerCard, beneficiaryId);

    Instant now = clock.instant();
    requests.save(LibrasRequest.register(beneficiaryId, now));
    metrics.counter("support.libras-request.registered").increment();
    log.info("libras service request registered for a beneficiary");

    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.LIBRAS_REQUEST_REGISTERED,
            authorAccountIdFor(authorEmail),
            beneficiaryId,
            Map.of(),
            auditContext));

    boolean open = LibrasOperatingHours.isOpenAt(ZonedDateTime.ofInstant(now, clock.getZone()));
    return open
        ? new LibrasRequestResponse(
            LibrasSituation.REGISTERED, LibrasRequestResponse.VIDEOCALL_SHORTLY, null)
        : new LibrasRequestResponse(
            LibrasSituation.REGISTERED,
            LibrasRequestResponse.NEXT_PERIOD,
            LibrasOperatingHours.HOURS_LABEL);
  }

  private UUID authorAccountIdFor(String email) {
    return identityAccounts.findByEmail(email).map(AccountCredentials::accountId).orElse(null);
  }
}
