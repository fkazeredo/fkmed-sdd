package com.fkmed.domain.plan;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a contact update would leave the contact e-mail empty (SPEC-0006 BR6). The contact
 * e-mail is a mandatory notification target and reimbursement prerequisite, so it can never be
 * emptied. Mapped to HTTP 422 by {@code infra.web.HttpErrorMapping}.
 */
public class ContactEmailRequiredException extends DomainException {

  public static final String CODE = "profile.contact-email-required";

  public ContactEmailRequiredException() {
    super(CODE);
  }
}
