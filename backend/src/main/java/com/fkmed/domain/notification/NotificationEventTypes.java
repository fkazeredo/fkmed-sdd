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

  /** A reimbursement request was submitted (business, opt-outable — SPEC-0015 §Events). */
  public static final String REIMBURSEMENT_SUBMITTED = "reimbursement.submitted";

  public static final String REIMBURSEMENT_PENDENCY_OPENED = "reimbursement.pendency-opened";

  public static final String REIMBURSEMENT_PENDENCY_RESOLVED = "reimbursement.pendency-resolved";

  public static final String REIMBURSEMENT_APPROVED = "reimbursement.approved";

  public static final String REIMBURSEMENT_DENIED = "reimbursement.denied";

  public static final String REIMBURSEMENT_PAYMENT_FAILED = "reimbursement.payment-failed";

  public static final String REIMBURSEMENT_CANCELLED = "reimbursement.cancelled";

  public static final String PREVIEW_CONCLUDED = "preview.concluded";

  /** A guide changed status (business, opt-outable). */
  public static final String GUIDE_STATUS_CHANGED = "guide.status-changed";

  /** An appointment was confirmed (business, opt-outable). */
  public static final String APPOINTMENT_CONFIRMED = "appointment.confirmed";

  /** An appointment was cancelled (business, opt-outable — SPEC-0009 BR9). */
  public static final String APPOINTMENT_CANCELLED = "appointment.cancelled";

  /** An appointment was rescheduled (business, opt-outable — SPEC-0009 BR10). */
  public static final String APPOINTMENT_RESCHEDULED = "appointment.rescheduled";

  /** It is the beneficiary's turn in the telemedicine queue (business — SPEC-0010 BR8). */
  public static final String TELE_TURN_REACHED = "tele.turn-reached";

  /** A telemedicine session was closed with its summary (business — SPEC-0010 BR9). */
  public static final String TELE_SESSION_CLOSED = "tele.session-closed";

  /** A clinical document was issued into Minha Saúde (business — SPEC-0011 §Events). */
  public static final String CLINICAL_DOCUMENT_ISSUED = "clinical-document.issued";

  /** A new invoice was issued for the contract titular (business — SPEC-0013 §Events). */
  public static final String FINANCE_INVOICE_ISSUED = "finance.invoice-issued";

  private NotificationEventTypes() {}
}
