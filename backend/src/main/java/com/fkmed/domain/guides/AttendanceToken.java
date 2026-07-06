package com.fkmed.domain.guides;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * A beneficiary attendance token (SPEC-0012 BR9-BR12): a short-lived 6-digit code presented at
 * reception. Only one non-invalidated token exists per beneficiary at a time — enforced at the
 * database by a partial unique index on {@code beneficiary_id where invalidated_at is null}
 * (mirroring {@code domain.telemedicine}'s single-active-session index); generating a new token
 * invalidates the previous one first ({@link GuideService}'s sibling, {@link
 * com.fkmed.domain.guides.TokenService}, does so in one transaction).
 */
@Entity
@Table(name = "attendance_token")
@Getter
public class AttendanceToken {

  /** Validity window from generation (BR9). */
  static final Duration VALIDITY = Duration.ofMinutes(10);

  @Id private UUID id;

  @Column(nullable = false)
  private String code;

  @Column(name = "beneficiary_id", nullable = false)
  private UUID beneficiaryId;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "invalidated_at")
  private Instant invalidatedAt;

  @Column(name = "created_by")
  private UUID createdBy;

  /** JPA only. */
  protected AttendanceToken() {}

  /** Generates a fresh token for {@code beneficiaryId}, valid for {@link #VALIDITY} (BR9). */
  static AttendanceToken generate(UUID beneficiaryId, String code, Instant now, UUID createdBy) {
    AttendanceToken token = new AttendanceToken();
    token.id = UUID.randomUUID();
    token.code = code;
    token.beneficiaryId = beneficiaryId;
    token.generatedAt = now;
    token.expiresAt = now.plus(VALIDITY);
    token.createdBy = createdBy;
    return token;
  }

  /** Invalidates this token immediately, ahead of its natural expiry (BR9). */
  void invalidate(Instant now) {
    this.invalidatedAt = now;
  }

  /** Whether this token is still usable: not invalidated and not past {@link #expiresAt}. */
  boolean isActive(Instant now) {
    return invalidatedAt == null && now.isBefore(expiresAt);
  }
}
