package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/** Bank correction is allowed only after a failed reimbursement payment. */
public class ReimbursementCorrectionNotAllowedException extends DomainException {

  public static final String CODE = "reimbursement.correction-not-allowed";

  public ReimbursementCorrectionNotAllowedException() {
    super(CODE);
  }
}
