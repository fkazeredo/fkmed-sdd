package com.fkmed.domain.telemedicine;

import com.fkmed.domain.error.DomainException;

/**
 * The triage carries an unknown symptom code or a duration outside the fixed list (SPEC-0010 BR2,
 * §Validation Rules). The frontend only submits catalog values, so this is a defensive boundary
 * check for malformed input. Maps to {@code 422 tele.triage-invalid}.
 */
public class TeleTriageInvalidException extends DomainException {

  public static final String CODE = "tele.triage-invalid";

  public TeleTriageInvalidException() {
    super(CODE);
  }
}
