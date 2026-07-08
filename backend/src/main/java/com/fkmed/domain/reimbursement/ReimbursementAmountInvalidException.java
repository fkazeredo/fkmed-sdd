package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * The amount paid is not greater than zero (SPEC-0015 BR6): 422 {@code
 * reimbursement.amount-invalid}.
 */
public class ReimbursementAmountInvalidException extends DomainException {

  public static final String CODE = "reimbursement.amount-invalid";

  public ReimbursementAmountInvalidException() {
    super(CODE);
  }
}
