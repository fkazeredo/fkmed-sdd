package com.fkmed.domain.appointment;

import com.fkmed.domain.error.DomainException;

/**
 * An exam confirmation arrived without the mandatory medical-order attachment (SPEC-0009 BR4/AC2).
 * Maps to {@code 422 appointment.attachment-required}.
 */
public class MedicalOrderRequiredException extends DomainException {

  public static final String CODE = "appointment.attachment-required";

  public MedicalOrderRequiredException() {
    super(CODE);
  }
}
