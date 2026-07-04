package com.fkmed.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An immutable audit-trail entry (SPEC-0003 BR6/BR7). Created only through {@link #of} and never
 * mutated — the entity exposes no setter and no state transition, which is what makes the trail
 * append-only at the code level (the DB has no update/delete path either, save the retention
 * sweep).
 */
@Entity
@Table(name = "audit_event")
@Getter
public class AuditEvent {

  @Id private UUID id;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "author_account_id")
  private UUID authorAccountId;

  @Column(name = "target_beneficiary_id")
  private UUID targetBeneficiaryId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details", nullable = false)
  private Map<String, String> details;

  @Column(name = "ip")
  private String ip;

  @Column(name = "user_agent")
  private String userAgent;

  /** JPA only. */
  protected AuditEvent() {}

  private AuditEvent(UUID id, Instant occurredAt, AuditEntry entry) {
    this.id = id;
    this.occurredAt = occurredAt;
    this.authorAccountId = entry.authorAccountId();
    this.targetBeneficiaryId = entry.targetBeneficiaryId();
    this.eventType = entry.eventType();
    this.details = entry.details();
    this.ip = entry.context().ip();
    this.userAgent = entry.context().userAgent();
  }

  /** Materializes an audit row from a recording request stamped at {@code occurredAt}. */
  static AuditEvent of(AuditEntry entry, Instant occurredAt) {
    return new AuditEvent(UUID.randomUUID(), occurredAt, entry);
  }
}
