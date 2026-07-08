package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * An attachment's real content is not JPG/PNG/PDF (SPEC-0015 BR8, magic-byte checked via {@code
 * domain.upload.FileContentType}): 422 {@code reimbursement.document-invalid-content}.
 */
public class ReimbursementDocumentInvalidContentException extends DomainException {

  public static final String CODE = "reimbursement.document-invalid-content";

  public ReimbursementDocumentInvalidContentException() {
    super(CODE);
  }
}
