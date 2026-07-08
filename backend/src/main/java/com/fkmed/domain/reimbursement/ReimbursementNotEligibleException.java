package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * The caller's plan has no reimbursement right (SPEC-0015 BR1): 403 {@code
 * reimbursement.not-eligible}.
 */
public class ReimbursementNotEligibleException extends DomainException {

  public static final String CODE = "reimbursement.not-eligible";

  public ReimbursementNotEligibleException() {
    super(CODE);
  }
}
