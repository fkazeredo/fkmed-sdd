package com.fkmed.domain.audit;

/**
 * Stable audit event-type codes (SPEC-0003 BR6 — {@code *Codes} constants, not an enum, baseline
 * §0019). The slice-1.1 subset covers the identity events of SPEC-0002 BR14; later slices add their
 * own codes to this class (registration changes, term acceptances, reimbursement transitions…).
 */
public final class AuditEventTypes {

  /** A first-access account was created (still {@code EMAIL_NOT_VERIFIED}). */
  public static final String ACCOUNT_CREATED = "identity.account-created";

  /** An account's e-mail was verified and the account activated. */
  public static final String EMAIL_VERIFIED = "identity.email-verified";

  /** A login attempt succeeded. */
  public static final String LOGIN_SUCCESS = "identity.login-success";

  /** A login attempt failed (credentials never recorded). */
  public static final String LOGIN_FAILURE = "identity.login-failure";

  /** A user logged out. */
  public static final String LOGOUT = "identity.logout";

  /** An account was locked after 5 consecutive failed logins (SPEC-0002 BR8). */
  public static final String ACCOUNT_LOCKED = "identity.account-locked";

  /** A password recovery was requested for an existing account (SPEC-0002 BR10). */
  public static final String PASSWORD_RECOVERY_REQUESTED = "identity.password-recovery-requested";

  /** An account's password changed via recovery reset or authenticated change (BR10/BR11). */
  public static final String PASSWORD_CHANGED = "identity.password-changed";

  /**
   * A titular viewed a dependent's digital card (SPEC-0003 BR6, SPEC-0007 BR7) — sensitive-data
   * access (CNS in full, BR8) that must be auditable. Never recorded for a self-view.
   */
  public static final String DEPENDENT_CARD_VIEWED = "card.dependent-viewed";

  private AuditEventTypes() {}
}
