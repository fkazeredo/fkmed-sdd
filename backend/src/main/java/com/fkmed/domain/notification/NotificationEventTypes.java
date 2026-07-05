package com.fkmed.domain.notification;

/**
 * Stable notification event-type codes (SPEC-0004 BR5 — {@code *Codes} constants, not an enum,
 * baseline §0019). These mirror the {@code notification_event_type} rows seeded by Flyway V10 and
 * are the wiring points producers reference. Account/security types are mandatory (BR7); business
 * types are opt-outable. Adding a type is a migration + a constant here, never an enum value.
 */
public final class NotificationEventTypes {

  /** An account's password changed (mandatory; e-mail sent by the identity seam — DL-0008). */
  public static final String ACCOUNT_PASSWORD_CHANGED = "account.password-changed";

  /** An account was locked after repeated failed logins (mandatory). */
  public static final String ACCOUNT_LOCKED = "account.locked";

  /** An account's contact data changed (mandatory). */
  public static final String ACCOUNT_CONTACT_CHANGED = "account.contact-changed";

  /** A reimbursement was paid (business, opt-outable). */
  public static final String REIMBURSEMENT_PAID = "reimbursement.paid";

  /** A guide changed status (business, opt-outable). */
  public static final String GUIDE_STATUS_CHANGED = "guide.status-changed";

  /** An appointment was confirmed (business, opt-outable). */
  public static final String APPOINTMENT_CONFIRMED = "appointment.confirmed";

  private NotificationEventTypes() {}
}
