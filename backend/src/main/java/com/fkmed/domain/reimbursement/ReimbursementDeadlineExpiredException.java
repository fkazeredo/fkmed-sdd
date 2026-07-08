package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * The care date is more than 12 months before the request date (SPEC-0015 BR5, AC3): 422 {@code
 * reimbursement.deadline-expired}.
 */
public class ReimbursementDeadlineExpiredException extends DomainException {

  public static final String CODE = "reimbursement.deadline-expired";

  public ReimbursementDeadlineExpiredException() {
    super(CODE);
  }
}
