package com.fkmed.domain.plan;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a contact update carries a UF absent from the seeded UF registry (SPEC-0006
 * §Validation Rules, baseline §0019). Mapped to HTTP 422 by {@code infra.web.HttpErrorMapping}.
 */
public class UfInvalidException extends DomainException {

  public static final String CODE = "profile.uf-invalid";

  public UfInvalidException() {
    super(CODE);
  }
}
