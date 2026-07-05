package com.fkmed.domain.plan;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a contact update would leave the mobile phone empty (SPEC-0006 BR6). The mobile is a
 * mandatory notification target and reimbursement prerequisite, so it can never be emptied. Mapped
 * to HTTP 422 by {@code infra.web.HttpErrorMapping}.
 */
public class MobileRequiredException extends DomainException {

  public static final String CODE = "profile.mobile-required";

  public MobileRequiredException() {
    super(CODE);
  }
}
