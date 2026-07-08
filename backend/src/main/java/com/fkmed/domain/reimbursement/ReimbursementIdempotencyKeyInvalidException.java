package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/** Raised when the submission idempotency key is absent, blank or too long (SPEC-0015 BR13). */
public class ReimbursementIdempotencyKeyInvalidException extends DomainException {

  public static final String CODE = "reimbursement.idempotency-key-invalid";

  public ReimbursementIdempotencyKeyInvalidException() {
    super(CODE);
  }
}
