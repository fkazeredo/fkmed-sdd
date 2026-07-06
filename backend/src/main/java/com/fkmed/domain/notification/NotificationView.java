package com.fkmed.domain.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model of one notification in the list (SPEC-0004 §Input/Output). {@code type} is the event
 * type's stable code; {@code read} is derived from the read timestamp; {@code link} may be null.
 */
public record NotificationView(
    UUID id, String type, String title, String body, String link, Instant createdAt, boolean read) {

  /** Projects a {@link Notification} entity into its API view. */
  public static NotificationView from(Notification notification) {
    return new NotificationView(
        notification.getId(),
        notification.getEventTypeCode(),
        notification.getTitle(),
        notification.getBody(),
        notification.getLink(),
        notification.getCreatedAt(),
        notification.isRead());
  }
}
