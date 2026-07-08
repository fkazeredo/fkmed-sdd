package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/** The requested transition is outside the SPEC-0016 state machine. */
public class ReimbursementInvalidTransitionException extends DomainException {

  public static final String CODE = "reimbursement.invalid-transition";

  public ReimbursementInvalidTransitionException() {
    super(CODE);
  }
}
