package com.fkmed.domain.identity;

import com.fkmed.domain.error.DomainException;

/**
 * The chosen password violates the policy (SPEC-0002 BR9 / RN-AUTH-01): minimum 8 characters, at
 * least one letter and one digit, not equal to the login e-mail and not in the common-passwords
 * list. The message stays generic — it never says which rule failed (the client mirrors the rules,
 * BR16).
 */
public final class PasswordPolicyViolationException extends DomainException {

  public static final String CODE = "auth.password-policy-violation";

  public PasswordPolicyViolationException() {
    super(CODE);
  }
}
