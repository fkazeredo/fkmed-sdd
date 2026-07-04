package com.fkmed.domain.identity;

import com.fkmed.domain.error.DomainException;

/** The login e-mail is already in use by another account (SPEC-0002 BR4). */
public final class EmailAlreadyUsedException extends DomainException {

  public static final String CODE = "auth.email-already-used";

  public EmailAlreadyUsedException() {
    super(CODE);
  }
}
