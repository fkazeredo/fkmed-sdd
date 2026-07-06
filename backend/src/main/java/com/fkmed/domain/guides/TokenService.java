package com.fkmed.domain.guides;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.BeneficiaryNotAccessibleException;
import io.micrometer.core.instrument.MeterRegistry;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service and public facade of the attendance-token aggregate (SPEC-0012 BR9-BR12): a
 * 6-digit, cryptographically random code valid for 10 minutes, one non-invalidated token per
 * beneficiary at a time. Family scope is enforced through {@link BeneficiaryAccess} (SPEC-0003
 * BR3); a titular generating a token for a dependent is audited (BR12/SPEC-0003 BR4).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final AttendanceTokenRepository tokens;
  private final BeneficiaryAccess beneficiaryAccess;
  private final IdentityAccounts identityAccounts;
  private final AuditRecorder auditRecorder;
  private final MeterRegistry metrics;
  private final Clock clock;

  /**
   * Generates a fresh token for {@code beneficiaryId}, invalidating any previous active token first
   * (BR9). Audits the authorship when the caller (a titular) generates it for a dependent
   * (BR12/SPEC-0003 BR4).
   *
   * @throws com.fkmed.domain.plan.BeneficiaryNotAccessibleException when the beneficiary is out of
   *     the caller's scope.
   */
  @Transactional
  public TokenView generate(
      String callerCard, String authorEmail, UUID beneficiaryId, AuditContext auditContext) {
    List<AccessibleBeneficiary> accessible = beneficiaryAccess.accessibleFor(callerCard);
    boolean inScope =
        accessible.stream().anyMatch(candidate -> candidate.beneficiaryId().equals(beneficiaryId));
    if (!inScope) {
      throw new BeneficiaryNotAccessibleException();
    }

    Instant now = clock.instant();
    tokens
        .findByBeneficiaryIdAndInvalidatedAtIsNull(beneficiaryId)
        .ifPresent(
            existing -> {
              existing.invalidate(now);
              // Flushed immediately (not just saved): Hibernate's default flush ordering executes
              // pending INSERTs before UPDATEs, so without this the new token's insert would race
              // the previous token's invalidation and trip the partial unique index
              // (uq_attendance_token_active) — regression coverage in TokenApiIT.
              tokens.saveAndFlush(existing);
            });

    UUID authorAccountId = authorAccountIdFor(authorEmail);
    AttendanceToken token =
        AttendanceToken.generate(beneficiaryId, newCode(), now, authorAccountId);
    tokens.save(token);
    metrics.counter("token.generated").increment();
    log.info("attendance token generated for a beneficiary");

    if (isForDependent(accessible, beneficiaryId)) {
      auditRecorder.record(
          new AuditEntry(
              AuditEventTypes.DEPENDENT_TOKEN_GENERATED,
              authorAccountId,
              beneficiaryId,
              Map.of(),
              auditContext));
    }
    return new TokenView(token.getCode(), token.getExpiresAt());
  }

  /**
   * The beneficiary's current valid token (BR9/BR10).
   *
   * @throws com.fkmed.domain.plan.BeneficiaryNotAccessibleException when the beneficiary is out of
   *     the caller's scope.
   * @throws TokenNoneActiveException when there is no non-invalidated, non-expired token.
   */
  @Transactional(readOnly = true)
  public TokenView current(String callerCard, UUID beneficiaryId) {
    beneficiaryAccess.requireAccessible(callerCard, beneficiaryId);
    Instant now = clock.instant();
    return tokens
        .findByBeneficiaryIdAndInvalidatedAtIsNull(beneficiaryId)
        .filter(token -> token.isActive(now))
        .map(token -> new TokenView(token.getCode(), token.getExpiresAt()))
        .orElseThrow(TokenNoneActiveException::new);
  }

  private UUID authorAccountIdFor(String email) {
    return identityAccounts.findByEmail(email).map(AccountCredentials::accountId).orElse(null);
  }

  private static boolean isForDependent(
      List<AccessibleBeneficiary> accessible, UUID beneficiaryId) {
    if (accessible.isEmpty()) {
      return false;
    }
    UUID selfId = accessible.get(0).beneficiaryId();
    return !beneficiaryId.equals(selfId);
  }

  /** A fresh 6-digit code, zero-padded, drawn from a {@link SecureRandom} (Validation Rules). */
  private static String newCode() {
    return "%06d".formatted(RANDOM.nextInt(1_000_000));
  }
}
