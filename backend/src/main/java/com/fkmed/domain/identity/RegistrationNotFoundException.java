package com.fkmed.domain.identity;

import com.fkmed.domain.error.DomainException;

/**
 * The identity triple (CPF + card + birth date) matched no active beneficiary, or the registration
 * token is invalid/expired (SPEC-0002 BR1). A single generic refusal — never reveals which field
 * diverged.
 */
public final class RegistrationNotFoundException extends DomainException {

  public static final String CODE = "auth.registration-not-found";

  public RegistrationNotFoundException() {
    super(CODE);
  }
}
