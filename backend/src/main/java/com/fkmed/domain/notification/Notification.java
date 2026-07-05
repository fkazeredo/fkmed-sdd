package com.fkmed.domain.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * One in-app notification delivered to an account (SPEC-0004 BR1). Immutable content once created;
 * the only mutation is the read transition ({@link #markRead(Instant)}), so read state is simply
 * the nullability of {@code readAt} (BR2). Title and body are length-bounded at creation (title ≤
 * {@value #MAX_TITLE_LENGTH}, body ≤ {@value #MAX_BODY_LENGTH}) and MUST already be free of full
 * sensitive data (BR4 — the producer masks). The {@code accountId} is a cross-context id value (no
 * JPA relationship crosses the module — ADR-0001).
 */
@Entity
@Table(name = "notification")
@Getter
public class Notification {

  /** Maximum title length enforced at creation (SPEC-0004 §Validation Rules). */
  public static final int MAX_TITLE_LENGTH = 120;

  /** Maximum body length enforced at creation (SPEC-0004 §Validation Rules). */
  public static final int MAX_BODY_LENGTH = 500;

  @Id private UUID id;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Column(name = "event_type_code", nullable = false, updatable = false)
  private String eventTypeCode;

  @Column(nullable = false, updatable = false)
  private String title;

  @Column(nullable = false, updatable = false)
  private String body;

  @Column(updatable = false)
  private String link;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "read_at")
  private Instant readAt;

  /** JPA only. */
  protected Notification() {}

  private Notification(
      UUID id,
      UUID accountId,
      String eventTypeCode,
      String title,
      String body,
      String link,
      Instant createdAt) {
    if (accountId == null) {
      throw new IllegalArgumentException("notification accountId is required");
    }
    if (eventTypeCode == null || eventTypeCode.isBlank()) {
      throw new IllegalArgumentException("notification eventTypeCode is required");
    }
    if (title == null || title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
      throw new IllegalArgumentException("notification title is required and must be ≤ 120 chars");
    }
    if (body == null || body.isBlank() || body.length() > MAX_BODY_LENGTH) {
      throw new IllegalArgumentException("notification body is required and must be ≤ 500 chars");
    }
    if (createdAt == null) {
      throw new IllegalArgumentException("notification createdAt is required");
    }
    this.id = id;
    this.accountId = accountId;
    this.eventTypeCode = eventTypeCode;
    this.title = title;
    this.body = body;
    this.link = link;
    this.createdAt = createdAt;
  }

  /**
   * Creates an unread in-app notification, enforcing the title/body length limits.
   *
   * @throws IllegalArgumentException when a required field is missing or a length limit is
   *     exceeded.
   */
  public static Notification create(
      UUID accountId,
      String eventTypeCode,
      String title,
      String body,
      String link,
      Instant createdAt) {
    return new Notification(
        UUID.randomUUID(), accountId, eventTypeCode, title, body, link, createdAt);
  }

  /** Marks the notification read at the given instant; idempotent — a re-read keeps the first. */
  public void markRead(Instant when) {
    if (this.readAt == null) {
      this.readAt = when;
    }
  }

  /** Whether the notification has been read (BR2). */
  public boolean isRead() {
    return this.readAt != null;
  }
}
