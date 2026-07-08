package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/** Unknown or out-of-scope reimbursement preview. */
public class PreviewNotFoundException extends DomainException {

  public static final String CODE = "preview.not-found";

  public PreviewNotFoundException() {
    super(CODE);
  }
}
