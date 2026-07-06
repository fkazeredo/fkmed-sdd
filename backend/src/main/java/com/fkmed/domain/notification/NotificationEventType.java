package com.fkmed.domain.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A notification event type — registry (reference) data, NOT an enum (SPEC-0004 BR5, baseline
 * §0019): {@code code} is a stable identifier referenced across specs, {@code description} the
 * editable pt-BR label shown on the preferences screen, {@code emailDefault} whether the type
 * e-mails by default and {@code mandatory} whether its e-mail channel may be disabled (BR7). Seeded
 * by Flyway V10; read-only at runtime in this phase.
 */
@Entity
@Table(name = "notification_event_type")
@Getter
public class NotificationEventType {

  @Id private String code;

  @Column(nullable = false)
  private String description;

  @Column(name = "email_default", nullable = false)
  private boolean emailDefault;

  @Column(nullable = false)
  private boolean mandatory;

  /** JPA only. */
  protected NotificationEventType() {}

  /**
   * Resolves whether e-mail is due for this type given the user's opt-out (SPEC-0004 BR7). A
   * mandatory type ignores the opt-out (it cannot be disabled); a non-mandatory type e-mails only
   * when it defaults to e-mail and the user has not opted out. In-app delivery is always on and is
   * not governed here.
   */
  public boolean emailEnabledWhenOptedOut(boolean optedOut) {
    return emailEnabled(mandatory, emailDefault, optedOut);
  }

  /**
   * Pure e-mail-channel resolution (SPEC-0004 BR7), extracted for unit testing: a mandatory type
   * ignores the opt-out; a non-mandatory type e-mails only when it defaults to e-mail and is not
   * opted out.
   */
  static boolean emailEnabled(boolean mandatory, boolean emailDefault, boolean optedOut) {
    return mandatory ? emailDefault : (emailDefault && !optedOut);
  }
}
