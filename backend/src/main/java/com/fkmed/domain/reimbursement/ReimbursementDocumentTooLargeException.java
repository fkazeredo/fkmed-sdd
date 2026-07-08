package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * A single attachment exceeds 2 MB (SPEC-0015 BR8, AC2): 422 {@code
 * reimbursement.document-too-large}.
 */
public class ReimbursementDocumentTooLargeException extends DomainException {

  public static final String CODE = "reimbursement.document-too-large";

  public ReimbursementDocumentTooLargeException() {
    super(CODE);
  }
}
