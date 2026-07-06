package com.fkmed.domain.appointment;

import com.fkmed.domain.error.DomainException;

/**
 * The beneficiary already holds an active appointment ({@code AGENDADO}/{@code REAGENDADO}) at the
 * same date and time (SPEC-0009 BR8/AC5). Maps to {@code 409 appointment.time-conflict}.
 */
public class AppointmentTimeConflictException extends DomainException {

  public static final String CODE = "appointment.time-conflict";

  public AppointmentTimeConflictException() {
    super(CODE);
  }
}
