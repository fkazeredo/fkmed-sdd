package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * The requester's contact e-mail or mobile is missing (SPEC-0015 BR2, SPEC-0006 BR6): 409 {@code
 * reimbursement.contacts-missing}.
 */
public class ReimbursementContactsMissingException extends DomainException {

  public static final String CODE = "reimbursement.contacts-missing";

  public ReimbursementContactsMissingException() {
    super(CODE);
  }
}
