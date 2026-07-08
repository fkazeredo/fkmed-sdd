package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * The sum of Terapia/Psicologia session amounts does not equal the informed total (SPEC-0015 BR7,
 * AC4): 422 {@code reimbursement.sessions-sum-mismatch}.
 */
public class ReimbursementSessionsSumMismatchException extends DomainException {

  public static final String CODE = "reimbursement.sessions-sum-mismatch";

  public ReimbursementSessionsSumMismatchException() {
    super(CODE);
  }
}
