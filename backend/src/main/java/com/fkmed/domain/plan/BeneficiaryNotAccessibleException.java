package com.fkmed.domain.plan;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when the authenticated caller requests a beneficiary outside their family scope (SPEC-0003
 * BR2/BR3, DL-0004). Mapped to HTTP 404 by {@code infra.web.HttpErrorMapping} so the response never
 * reveals that the beneficiary exists — access outside the caller's scope must look identical to a
 * beneficiary that is not there (BR2).
 */
public class BeneficiaryNotAccessibleException extends DomainException {

  public static final String CODE = "context.beneficiary-not-accessible";

  public BeneficiaryNotAccessibleException() {
    super(CODE);
  }
}
