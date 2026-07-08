package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * The submitted bank data is not a personal (PF) account of the plan titular — a non-titular
 * requester, a third-party/salary/PJ account (SPEC-0015 BR11): 422 {@code
 * reimbursement.bank-account-not-allowed}.
 */
public class ReimbursementBankAccountNotAllowedException extends DomainException {

  public static final String CODE = "reimbursement.bank-account-not-allowed";

  public ReimbursementBankAccountNotAllowedException() {
    super(CODE);
  }
}
