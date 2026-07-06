package com.fkmed.domain.plan;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a contact update carries a (non-empty) CEP that is not exactly 8 digits (SPEC-0006
 * §Validation Rules). Mapped to HTTP 422 by {@code infra.web.HttpErrorMapping}.
 */
public class CepInvalidException extends DomainException {

  public static final String CODE = "profile.cep-invalid";

  public CepInvalidException() {
    super(CODE);
  }
}
