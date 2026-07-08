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

  /** A beneficiary's contact/address data was changed (SPEC-0006 BR7). */
  public static final String CONTACT_DATA_CHANGED = "profile.contact-data-changed";

  /** A beneficiary's profile photo was uploaded, replaced or removed (SPEC-0006 BR2/BR3). */
  public static final String PROFILE_PHOTO_CHANGED = "profile.photo-changed";

  /** A user accepted a legal-document version (SPEC-0006 BR8). */
  public static final String TERM_ACCEPTED = "legal.term-accepted";

  /**
   * A titular viewed a dependent's clinical document — list filtered to the dependent, detail or
   * PDF (SPEC-0011 BR9) — sensitive clinical content that must be auditable. Never recorded for a
   * self-view.
   */
  public static final String DEPENDENT_CLINICAL_DOCUMENT_VIEWED = "clinicaldocs.dependent-viewed";

  /**
   * An operator-simulation action was executed (SPEC-0018 BR3): every {@code /api/sim/**} call is
   * audited with the operator as author, so the simulated back-office action is traceable and
   * indistinguishable from a real one to the consuming modules.
   */
  public static final String OPERATOR_SIM_ACTION = "sim.operator-action";

  /**
   * A titular generated an attendance token for a dependent (SPEC-0012 BR12, SPEC-0003 BR4). Never
   * recorded when the beneficiary generates their own token.
   */
  public static final String DEPENDENT_TOKEN_GENERATED = "guides.dependent-token-generated";

  /**
   * A beneficiary registered a Libras service request (SPEC-0014 BR4/§Observability). Recorded for
   * every request, regardless of authorship (unlike {@link #DEPENDENT_TOKEN_GENERATED}).
   */
  public static final String SUPPORT_LIBRAS_REQUESTED = "support.libras-requested";

  /**
   * A beneficiary submitted a reimbursement request (SPEC-0015 BR15). Details include only
   * non-sensitive request metadata; provider and bank data must never be recorded here.
   */
  public static final String REIMBURSEMENT_SUBMITTED = "reimbursement.submitted";

  private AuditEventTypes() {}
}
