package com.fkmed.domain.identity;

import com.fkmed.domain.error.DomainException;

/**
 * The current password supplied to an authenticated password change did not match (SPEC-0002 BR11).
 * Maps to HTTP 422 Unprocessable Content (§Error Behavior).
 */
public final class CurrentPasswordIncorrectException extends DomainException {

  public static final String CODE = "auth.current-password-incorrect";

  public CurrentPasswordIncorrectException() {
    super(CODE);
  }
}
