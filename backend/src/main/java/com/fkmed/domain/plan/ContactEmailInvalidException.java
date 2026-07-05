package com.fkmed.domain.plan;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a contact update carries a malformed contact e-mail (SPEC-0006 §Validation Rules).
 * Mapped to HTTP 422 by {@code infra.web.HttpErrorMapping}.
 */
public class ContactEmailInvalidException extends DomainException {

  public static final String CODE = "profile.contact-email-invalid";

  public ContactEmailInvalidException() {
    super(CODE);
  }
}
