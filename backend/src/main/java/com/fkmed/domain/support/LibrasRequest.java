package com.fkmed.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * A registered Libras service request (SPEC-0014 BR4) — beneficiary + timestamp + situation, per
 * the module's §Observability audit requirement. Always created {@link
 * LibrasRequestSituation#REGISTERED}; the {@code ATTENDED} transition is a future operator-side
 * concern, out of scope for this POC.
 */
@Entity
@Table(name = "libras_request")
@Getter
public class LibrasRequest {

  @Id private UUID id;

  @Column(name = "beneficiary_id", nullable = false)
  private UUID beneficiaryId;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LibrasRequestSituation situation;

  /** JPA only. */
  protected LibrasRequest() {}

  /** Registers a fresh request (the only construction path) at {@code requestedAt}. */
  static LibrasRequest register(UUID beneficiaryId, Instant requestedAt) {
    Objects.requireNonNull(beneficiaryId, "beneficiaryId is required");
    Objects.requireNonNull(requestedAt, "requestedAt is required");
    LibrasRequest request = new LibrasRequest();
    request.id = UUID.randomUUID();
    request.beneficiaryId = beneficiaryId;
    request.requestedAt = requestedAt;
    request.situation = LibrasRequestSituation.REGISTERED;
    return request;
  }
}
