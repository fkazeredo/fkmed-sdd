package com.fkmed.domain.identity;

/**
 * Lifecycle of a {@link UserAccount} (SPEC-0002 BR5).
 *
 * <p>Kept as an enum (baseline §0019 keep-criterion: a <b>state machine</b> whose transitions the
 * code enforces — {@code EMAIL_NOT_VERIFIED → ACTIVE}). It is not reference data: the values carry
 * wired behavior (only an {@code ACTIVE} account may authenticate). Recovery/lock states arrive
 * with SLICE 1.2 and become their own columns, not new values here.
 */
public enum AccountStatus {

  /** Account created; the verification link has not been opened yet — login is refused (BR6). */
  EMAIL_NOT_VERIFIED,

  /** E-mail verified; the account may authenticate. */
  ACTIVE
}
