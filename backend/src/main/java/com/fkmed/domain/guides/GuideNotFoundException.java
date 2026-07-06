package com.fkmed.domain.guides;

import com.fkmed.domain.error.DomainException;

/**
 * The requested guide does not exist, or does not belong to the given (already scope-checked)
 * beneficiary (SPEC-0012 §Error Behavior) — the two cases are indistinguishable on purpose:
 * existence is never revealed to a caller without access.
 */
public class GuideNotFoundException extends DomainException {

  public static final String CODE = "guide.not-found";

  public GuideNotFoundException() {
    super(CODE);
  }
}
