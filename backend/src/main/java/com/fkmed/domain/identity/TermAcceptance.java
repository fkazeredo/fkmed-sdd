package com.fkmed.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * A recorded acceptance of a legal document version at registration (SPEC-0002 BR15). Immutable —
 * an acceptance is a historical fact; a new version yields a new row (SPEC-0006).
 */
@Entity
@Table(name = "term_acceptance")
@Getter
public class TermAcceptance {

  @Id private UUID id;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Column(name = "document_type", nullable = false, updatable = false)
  private String documentType;

  @Column(nullable = false, updatable = false)
  private String version;

  @Column(name = "accepted_at", nullable = false, updatable = false)
  private Instant acceptedAt;

  /** JPA only. */
  protected TermAcceptance() {}

  private TermAcceptance(
      UUID id, UUID accountId, String documentType, String version, Instant acceptedAt) {
    this.id = id;
    this.accountId = accountId;
    this.documentType = documentType;
    this.version = version;
    this.acceptedAt = acceptedAt;
  }

  /** Records acceptance of {@code documentType} at {@code version} at the given instant. */
  public static TermAcceptance record(
      UUID accountId, String documentType, String version, Instant acceptedAt) {
    return new TermAcceptance(UUID.randomUUID(), accountId, documentType, version, acceptedAt);
  }
}
