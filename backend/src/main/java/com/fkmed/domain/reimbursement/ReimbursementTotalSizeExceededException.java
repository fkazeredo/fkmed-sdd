package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * The attachment set exceeds 20 MB total (SPEC-0015 BR8, AC2): 422 {@code
 * reimbursement.total-size-exceeded}.
 */
public class ReimbursementTotalSizeExceededException extends DomainException {

  public static final String CODE = "reimbursement.total-size-exceeded";

  public ReimbursementTotalSizeExceededException() {
    super(CODE);
  }
}
