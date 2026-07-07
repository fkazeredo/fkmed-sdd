package com.fkmed.domain.finance;

import com.fkmed.domain.error.DomainException;

/**
 * The submitted line is not exactly 47 digits after normalization (SPEC-0013 BR4): 422 {@code
 * finance.line-invalid-format}, raised BEFORE any invoice lookup.
 */
public class LineInvalidFormatException extends DomainException {

  public static final String CODE = "finance.line-invalid-format";

  public LineInvalidFormatException() {
    super(CODE);
  }
}
