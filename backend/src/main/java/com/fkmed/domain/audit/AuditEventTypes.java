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

  private AuditEventTypes() {}
}
