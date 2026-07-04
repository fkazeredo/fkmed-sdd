package com.fkmed.domain.plan;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when the authenticated user has no active beneficiary/plan link (SPEC-0001 §Error Behavior
 * — mapped to HTTP 404 by {@code infra.web.HttpErrorMapping}).
 */
public class PlanNotFoundException extends DomainException {

  public static final String CODE = "plan.not-found";

  public PlanNotFoundException() {
    super(CODE);
  }
}
