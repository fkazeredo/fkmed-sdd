package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/** Unknown or out-of-scope reimbursement request. */
public class ReimbursementNotFoundException extends DomainException {

  public static final String CODE = "reimbursement.not-found";

  public ReimbursementNotFoundException() {
    super(CODE);
  }
}
