package com.fkmed.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A beneficiary's Central de Libras service request (SPEC-0014 BR4): the beneficiary, the instant
 * it was requested and its {@link LibrasSituation}. Always created as {@code REGISTERED} — {@code
 * ATTENDED} is a future operator-side transition, not writable in this slice.
 */
@Entity
@Table(name = "libras_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LibrasRequest {

  @Id private UUID id;

  @Column(name = "beneficiary_id", nullable = false)
  private UUID beneficiaryId;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LibrasSituation situation;

  LibrasRequest(UUID id, UUID beneficiaryId, Instant requestedAt) {
    if (beneficiaryId == null) {
      throw new IllegalArgumentException("beneficiaryId is required");
    }
    if (requestedAt == null) {
      throw new IllegalArgumentException("requestedAt is required");
    }
    this.id = id;
    this.beneficiaryId = beneficiaryId;
    this.requestedAt = requestedAt;
    this.situation = LibrasSituation.REGISTERED;
  }

  /** Registers a new Libras service request for {@code beneficiaryId} at {@code requestedAt}. */
  public static LibrasRequest register(UUID beneficiaryId, Instant requestedAt) {
    return new LibrasRequest(UUID.randomUUID(), beneficiaryId, requestedAt);
  }
}
