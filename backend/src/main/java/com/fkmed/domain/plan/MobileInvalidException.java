package com.fkmed.domain.plan;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a contact update carries a mobile phone that does not match {@code (99) 99999-9999}
 * (SPEC-0006 §Validation Rules). Mapped to HTTP 422 by {@code infra.web.HttpErrorMapping}.
 */
public class MobileInvalidException extends DomainException {

  public static final String CODE = "profile.mobile-invalid";

  public MobileInvalidException() {
    super(CODE);
  }
}
