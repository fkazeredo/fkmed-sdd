package com.fkmed.domain.telemedicine;

import com.fkmed.domain.error.DomainException;

/**
 * The main complaint is outside the allowed 10–500 characters (SPEC-0010 BR2, §Error Behavior).
 * Maps to {@code 422 tele.complaint-invalid}.
 */
public class TeleComplaintInvalidException extends DomainException {

  public static final String CODE = "tele.complaint-invalid";

  public TeleComplaintInvalidException() {
    super(CODE);
  }
}
