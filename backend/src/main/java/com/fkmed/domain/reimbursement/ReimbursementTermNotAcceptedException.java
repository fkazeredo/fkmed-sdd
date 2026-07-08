package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * The adhesion term was not accepted, or the accepted version is stale (SPEC-0015 BR3, AC1): 422
 * {@code reimbursement.term-not-accepted}.
 */
public class ReimbursementTermNotAcceptedException extends DomainException {

  public static final String CODE = "reimbursement.term-not-accepted";

  public ReimbursementTermNotAcceptedException() {
    super(CODE);
  }
}
