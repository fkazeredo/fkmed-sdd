package com.fkmed.domain.plan;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a contact update carries a (non-empty) landline that does not match {@code (99)
 * 9999-9999} (SPEC-0006 §Validation Rules — landline is optional but format-checked when present).
 * Mapped to HTTP 422 by {@code infra.web.HttpErrorMapping}.
 */
public class LandlineInvalidException extends DomainException {

  public static final String CODE = "profile.landline-invalid";

  public LandlineInvalidException() {
    super(CODE);
  }
}
