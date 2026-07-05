package com.fkmed.domain.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

/**
 * A user's e-mail preference for one event type (SPEC-0004 BR7): the presence of a row with {@code
 * emailOptOut = true} suppresses the e-mail channel for that (account, type). Absence means the
 * type's default applies. Mandatory types can never be opted out — enforced by {@link
 * Notifications#updatePreferences}, this entity only stores the flag.
 */
@Entity
@Table(name = "notification_preference")
@IdClass(NotificationPreferenceId.class)
@Getter
public class NotificationPreference {

  @Id
  @Column(name = "account_id")
  private UUID accountId;

  @Id
  @Column(name = "event_type_code")
  private String eventTypeCode;

  @Column(name = "email_opt_out", nullable = false)
  private boolean emailOptOut;

  /** JPA only. */
  protected NotificationPreference() {}

  private NotificationPreference(UUID accountId, String eventTypeCode, boolean emailOptOut) {
    this.accountId = accountId;
    this.eventTypeCode = eventTypeCode;
    this.emailOptOut = emailOptOut;
  }

  /** Creates a preference row for an (account, event type). */
  public static NotificationPreference of(
      UUID accountId, String eventTypeCode, boolean emailOptOut) {
    return new NotificationPreference(accountId, eventTypeCode, emailOptOut);
  }

  /** Sets whether the user opts out of the e-mail channel for this type (BR7). */
  public void changeEmailOptOut(boolean emailOptOut) {
    this.emailOptOut = emailOptOut;
  }
}
