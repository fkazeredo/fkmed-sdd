package com.fkmed.domain.reimbursement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * One append-only timeline entry (SPEC-0015 BR13, SPEC-0016 BR4) — a child of {@link
 * ReimbursementRequest}. This slice writes exactly one row per request, at submission; SPEC-0016
 * appends further events as the state machine advances. Never updated or deleted.
 */
@Entity
@Table(name = "reimbursement_timeline_event")
@Getter
public class TimelineEvent {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id", nullable = false)
  private ReimbursementRequest request;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReimbursementStatus status;

  @Column private String description;

  /** JPA only. */
  protected TimelineEvent() {}

  private TimelineEvent(
      ReimbursementRequest request,
      Instant occurredAt,
      ReimbursementStatus status,
      String description) {
    this.id = UUID.randomUUID();
    this.request = request;
    this.occurredAt = occurredAt;
    this.status = status;
    this.description = description;
  }

  /** Records a new timeline entry bound to {@code request}. */
  static TimelineEvent of(
      ReimbursementRequest request,
      Instant occurredAt,
      ReimbursementStatus status,
      String description) {
    return new TimelineEvent(request, occurredAt, status, description);
  }
}
