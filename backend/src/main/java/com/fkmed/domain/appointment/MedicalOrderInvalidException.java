package com.fkmed.domain.appointment;

import com.fkmed.domain.error.DomainException;

/**
 * The medical-order attachment failed content validation (SPEC-0009 BR4): its real bytes are not a
 * JPG, PNG or PDF, or it exceeds 5 MB. Maps to {@code 422 appointment.attachment-invalid}.
 */
public class MedicalOrderInvalidException extends DomainException {

  public static final String CODE = "appointment.attachment-invalid";

  public MedicalOrderInvalidException() {
    super(CODE);
  }
}
