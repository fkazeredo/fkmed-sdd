package com.fkmed.domain.network;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when the name search query has fewer than {@link #MIN_LENGTH} characters after trimming
 * (SPEC-0008 BR8, §Validation Rules). Mapped to HTTP 422 by {@code infra.web.HttpErrorMapping}.
 */
public class NetworkQueryTooShortException extends DomainException {

  public static final String CODE = "network.query-too-short";

  /** The minimum trimmed length the name search query must have (BR8). */
  public static final int MIN_LENGTH = 3;

  public NetworkQueryTooShortException() {
    super(CODE);
  }
}
