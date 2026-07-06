package com.fkmed.domain.clinicaldocs;

import com.fkmed.domain.error.DomainException;

/**
 * The requested document does not exist, or falls outside the caller's family scope (SPEC-0011 BR9)
 * — the two cases are indistinguishable on purpose: existence is never revealed to a caller without
 * access.
 */
public class ClinicalDocumentNotFoundException extends DomainException {

  public static final String CODE = "document.not-found";

  public ClinicalDocumentNotFoundException() {
    super(CODE);
  }
}
