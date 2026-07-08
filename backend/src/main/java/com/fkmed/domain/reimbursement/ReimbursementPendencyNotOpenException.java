package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/** Pendency resolution was attempted while no documentation pendency is open. */
public class ReimbursementPendencyNotOpenException extends DomainException {

  public static final String CODE = "reimbursement.pendency-not-open";

  public ReimbursementPendencyNotOpenException() {
    super(CODE);
  }
}
