package com.fkmed.domain.identity;

import com.fkmed.domain.error.DomainException;

/**
 * A dependent younger than 18 on the registration date attempted first access (SPEC-0002 BR3): they
 * are served through the titular's account and get no account of their own.
 */
public final class DependentUnderageException extends DomainException {

  public static final String CODE = "auth.dependent-underage";

  public DependentUnderageException() {
    super(CODE);
  }
}
