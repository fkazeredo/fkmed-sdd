package com.fkmed.domain.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service and public facade of the notification module (SPEC-0004). The single entry
 * point other modules use to raise a notification ({@link #notify}) and the delivery layer uses to
 * read/mark/list and manage preferences. In-app delivery is always performed; e-mail is decided by
 * the type/preference resolution (BR7) and delivered off-transaction via {@link
 * NotificationEmailRequested} (BR6).
 */
@Service
@RequiredArgsConstructor
public class Notifications {

  private final NotificationRepository notifications;
  private final NotificationEventTypeRepository eventTypes;
  private final NotificationPreferenceRepository preferences;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /**
   * Raises a notification: always creates the in-app item, then — when the type/preference
   * resolution is due and a recipient e-mail is present — publishes {@link
   * NotificationEmailRequested} for AFTER_COMMIT delivery (BR6/BR7).
   *
   * @return the id of the created in-app notification.
   * @throws IllegalArgumentException when the event type code is not in the seeded catalog.
   */
  @Transactional
  public UUID notify(NotificationRequest request) {
    NotificationEventType type =
        eventTypes
            .findById(request.eventTypeCode())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "unknown notification event type: " + request.eventTypeCode()));
    Instant now = clock.instant();
    Notification notification =
        notifications.save(
            Notification.create(
                request.accountId(),
                type.getCode(),
                request.title(),
                request.body(),
                request.link(),
                now));

    boolean optedOut =
        preferences
            .findByAccountIdAndEventTypeCode(request.accountId(), type.getCode())
            .map(NotificationPreference::isEmailOptOut)
            .orElse(false);
    if (type.emailEnabledWhenOptedOut(optedOut) && hasEmail(request.recipientEmail())) {
      events.publishEvent(
          new NotificationEmailRequested(
              request.recipientEmail(), request.title(), request.body(), type.getCode()));
    }
    return notification.getId();
  }

  /** One page of the account's notifications, newest first, plus the unread count (BR2/BR3). */
  @Transactional(readOnly = true)
  public NotificationListView list(UUID accountId, int page, int size) {
    List<NotificationView> items =
        notifications
            .findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(page, size))
            .stream()
            .map(NotificationView::from)
            .toList();
    int unread = notifications.countByAccountIdAndReadAtIsNull(accountId);
    return new NotificationListView(unread, items);
  }

  /**
   * Marks one of the account's notifications read (BR2).
   *
   * @throws NotificationNotFoundException when the id is unknown or owned by another account.
   */
  @Transactional
  public void markRead(UUID accountId, UUID notificationId) {
    Notification notification =
        notifications
            .findByIdAndAccountId(notificationId, accountId)
            .orElseThrow(NotificationNotFoundException::new);
    notification.markRead(clock.instant());
  }

  /** Marks every unread notification of the account read (BR2). */
  @Transactional
  public void markAllRead(UUID accountId) {
    notifications.markAllReadFor(accountId, clock.instant());
  }

  /**
   * The full event-type catalog with the account's opt-out state and each type's mandatory flag.
   */
  @Transactional(readOnly = true)
  public NotificationPreferencesView preferences(UUID accountId) {
    Map<String, Boolean> optOuts =
        preferences.findByAccountId(accountId).stream()
            .collect(
                Collectors.toMap(
                    NotificationPreference::getEventTypeCode,
                    NotificationPreference::isEmailOptOut));
    List<NotificationPreferenceView> views =
        eventTypes.findAll(Sort.by("code")).stream()
            .map(
                type ->
                    new NotificationPreferenceView(
                        type.getCode(),
                        type.getDescription(),
                        optOuts.getOrDefault(type.getCode(), false),
                        type.isMandatory()))
            .toList();
    return new NotificationPreferencesView(views);
  }

  /**
   * Applies e-mail opt-out changes for the account and returns the refreshed catalog. Unknown types
   * are ignored (only existing types are updatable); opting out of a mandatory type is rejected
   * before anything is persisted (BR7).
   *
   * @throws MandatoryPreferenceOptOutException when a mandatory type is opted out.
   */
  @Transactional
  public NotificationPreferencesView updatePreferences(
      UUID accountId, List<PreferenceUpdate> updates) {
    Map<String, NotificationEventType> catalog =
        eventTypes.findAll().stream()
            .collect(Collectors.toMap(NotificationEventType::getCode, Function.identity()));

    for (PreferenceUpdate update : updates) {
      NotificationEventType type = catalog.get(update.type());
      if (type != null && update.emailOptOut() && type.isMandatory()) {
        throw new MandatoryPreferenceOptOutException();
      }
    }
    for (PreferenceUpdate update : updates) {
      NotificationEventType type = catalog.get(update.type());
      if (type == null) {
        continue;
      }
      NotificationPreference preference =
          preferences
              .findByAccountIdAndEventTypeCode(accountId, type.getCode())
              .orElseGet(() -> NotificationPreference.of(accountId, type.getCode(), false));
      preference.changeEmailOptOut(update.emailOptOut());
      preferences.save(preference);
    }
    return preferences(accountId);
  }

  private static boolean hasEmail(String email) {
    return email != null && !email.isBlank();
  }
}
