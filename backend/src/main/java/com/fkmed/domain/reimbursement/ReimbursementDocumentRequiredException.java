package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * A mandatory document category (receipt always, medical order per BR9's matrix) is missing
 * (SPEC-0015 BR8/BR9): 422 {@code reimbursement.document-required}.
 */
public class ReimbursementDocumentRequiredException extends DomainException {

  public static final String CODE = "reimbursement.document-required";

  public ReimbursementDocumentRequiredException() {
    super(CODE);
  }
}
