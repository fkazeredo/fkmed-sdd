package com.fkmed.domain.notification;

/**
 * Domain event: an e-mail is due for a notification (SPEC-0004 BR6). Published by {@link
 * Notifications} inside the notification transaction and consumed AFTER_COMMIT by an infra adapter
 * over the {@code MailSender} port, so a mail-sender outage never rolls back the business
 * transaction that raised it. Carries only already-masked content (BR4).
 *
 * @param recipientEmail the delivery address.
 * @param subject the e-mail subject (the notification title).
 * @param body the e-mail body (the notification body).
 * @param eventTypeCode the originating event type (for observability/correlation).
 */
public record NotificationEmailRequested(
    String recipientEmail, String subject, String body, String eventTypeCode) {}
