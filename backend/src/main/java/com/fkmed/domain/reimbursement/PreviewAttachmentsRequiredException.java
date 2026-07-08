package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/** An analyzed reimbursement preview is missing its mandatory attachments. */
public class PreviewAttachmentsRequiredException extends DomainException {

  public static final String CODE = "preview.attachments-required";

  public PreviewAttachmentsRequiredException() {
    super(CODE);
  }
}
