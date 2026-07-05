/**
 * The notification module (SPEC-0004): one product-wide mechanism that turns domain events into
 * in-app notifications (bell + notification center) and, per user preference, e-mails.
 *
 * <p>Owns {@code notification}, the {@code notification_event_type} registry catalog and {@code
 * notification_preference} (Flyway V10). The {@link com.fkmed.domain.notification.Notifications}
 * facade creates the in-app item (always on, BR7), resolves the per-type e-mail preference (default
 * × opt-out × mandatory, BR7) and — when e-mail is due — publishes {@link
 * com.fkmed.domain.notification.NotificationEmailRequested}, delivered off-transaction by an infra
 * adapter over the {@code MailSender} port (BR6, AFTER_COMMIT). Reacts to identity's {@link
 * com.fkmed.domain.identity.PasswordChanged} to raise the account.password-changed item; other
 * producers are wired by their own specs at integration (DL-0008). Event types are registry data,
 * never an enum (BR5, baseline §0019). Module map: ADR-0001.
 */
@org.springframework.modulith.ApplicationModule(displayName = "notification")
package com.fkmed.domain.notification;
