package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * The care date (or a session date) is in the future (SPEC-0015 BR5/BR7): 422 {@code
 * reimbursement.care-date-invalid}.
 */
public class ReimbursementCareDateInvalidException extends DomainException {

  public static final String CODE = "reimbursement.care-date-invalid";

  public ReimbursementCareDateInvalidException() {
    super(CODE);
  }
}
