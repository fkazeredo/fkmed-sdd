package com.fkmed.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0004 §Validation Rules: title/body length limits and the idempotent read transition. */
class NotificationTest {

  private static final UUID ACCOUNT = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-07-05T10:00:00Z");

  @Test
  void createsUnread_withinLengthLimits() {
    Notification notification =
        Notification.create(ACCOUNT, "reimbursement.paid", "Reembolso pago", "Corpo", "/x", NOW);
    assertThat(notification.isRead()).isFalse();
    assertThat(notification.getReadAt()).isNull();
    assertThat(notification.getLink()).isEqualTo("/x");
  }

  @Test
  void rejectsTitleOver120() {
    String tooLong = "a".repeat(Notification.MAX_TITLE_LENGTH + 1);
    assertThatThrownBy(
            () -> Notification.create(ACCOUNT, "reimbursement.paid", tooLong, "Corpo", null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBodyOver500() {
    String tooLong = "b".repeat(Notification.MAX_BODY_LENGTH + 1);
    assertThatThrownBy(
            () -> Notification.create(ACCOUNT, "reimbursement.paid", "Título", tooLong, null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankRequiredFields() {
    assertThatThrownBy(
            () -> Notification.create(ACCOUNT, "reimbursement.paid", " ", "Corpo", null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Notification.create(ACCOUNT, " ", "Título", "Corpo", null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> Notification.create(null, "reimbursement.paid", "Título", "Corpo", null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void markRead_isIdempotent_keepingTheFirstInstant() {
    Notification notification =
        Notification.create(ACCOUNT, "reimbursement.paid", "Título", "Corpo", null, NOW);
    Instant first = NOW.plusSeconds(10);
    notification.markRead(first);
    notification.markRead(NOW.plusSeconds(99));
    assertThat(notification.isRead()).isTrue();
    assertThat(notification.getReadAt()).isEqualTo(first);
  }
}
