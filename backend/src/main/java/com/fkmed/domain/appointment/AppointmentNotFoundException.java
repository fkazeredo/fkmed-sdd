package com.fkmed.domain.appointment;

import com.fkmed.domain.error.DomainException;

/**
 * The appointment does not exist or falls outside the caller's family scope (SPEC-0009 BR1/BR13).
 * The two cases collapse into one response so existence is never revealed (SPEC-0003 BR2). Maps to
 * {@code 404 appointment.not-found}.
 */
public class AppointmentNotFoundException extends DomainException {

  public static final String CODE = "appointment.not-found";

  public AppointmentNotFoundException() {
    super(CODE);
  }
}
