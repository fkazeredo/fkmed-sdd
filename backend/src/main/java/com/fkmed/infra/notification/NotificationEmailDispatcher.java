package com.fkmed.infra.notification;

import com.fkmed.domain.notification.NotificationEmailRequested;
import com.fkmed.infra.email.MailMessage;
import com.fkmed.infra.email.MailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Driven adapter that delivers notification e-mails (SPEC-0004 BR6). Listens to {@link
 * NotificationEmailRequested} AFTER_COMMIT and sends via the {@code MailSender} port; best-effort —
 * a mail-sender outage is logged, never propagated, so it can neither roll back nor fail the
 * business transaction that raised the notification. The event carries already-masked content
 * (BR4).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailDispatcher {

  private final MailSender mailSender;

  /** Sends the notification e-mail; failures are swallowed after logging. */
  @TransactionalEventListener
  public void onNotificationEmailRequested(NotificationEmailRequested event) {
    try {
      mailSender.send(new MailMessage(event.recipientEmail(), event.subject(), event.body()));
      log.info("notification e-mail dispatched for type {}", event.eventTypeCode());
    } catch (RuntimeException e) {
      log.error("failed to send notification e-mail for type {}", event.eventTypeCode(), e);
    }
  }
}
