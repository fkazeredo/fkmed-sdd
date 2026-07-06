package com.fkmed.domain.appointment;

import com.fkmed.domain.error.DomainException;

/**
 * The appointment can no longer be changed because its start time has passed or it is already
 * closed (SPEC-0009 BR9): cancellation and rescheduling are both blocked once the appointment is no
 * longer an upcoming active commitment. Maps to {@code 409 appointment.cancel-too-late} (the frozen
 * error code for the "too late to change" condition, shared by cancel and reschedule).
 */
public class AppointmentTooLateException extends DomainException {

  public static final String CODE = "appointment.cancel-too-late";

  public AppointmentTooLateException() {
    super(CODE);
  }
}
