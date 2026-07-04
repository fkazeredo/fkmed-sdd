package com.fkmed.domain.identity;

import com.fkmed.domain.error.DomainException;

/**
 * The matched beneficiary already has an account (SPEC-0002 BR2): no second account is created; the
 * user is directed to login or password recovery.
 */
public final class AccountAlreadyExistsException extends DomainException {

  public static final String CODE = "auth.account-already-exists";

  public AccountAlreadyExistsException() {
    super(CODE);
  }
}
