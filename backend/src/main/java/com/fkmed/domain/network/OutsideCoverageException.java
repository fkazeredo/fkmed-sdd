package com.fkmed.domain.network;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a requested UF is outside the caller's plan coverage (SPEC-0008 BR4, DL-0014): an
 * ESTADUAL plan only ever offers its own UF; an unresolvable plan (no beneficiary link) offers
 * none. Mapped to HTTP 422 by {@code infra.web.HttpErrorMapping}.
 */
public class OutsideCoverageException extends DomainException {

  public static final String CODE = "network.outside-coverage";

  public OutsideCoverageException() {
    super(CODE);
  }
}
