package com.fkmed.domain.appointment;

import com.fkmed.domain.error.DomainException;

/**
 * The chosen slot is outside the bookable window (SPEC-0009 BR5 + DL-0013): a past date, a date
 * beyond today + 30 days, a time less than the 2-hour minimum antecedence, or a slot that is not on
 * the unit's agenda at all. Maps to {@code 422 appointment.outside-horizon}.
 */
public class AppointmentOutsideHorizonException extends DomainException {

  public static final String CODE = "appointment.outside-horizon";

  public AppointmentOutsideHorizonException() {
    super(CODE);
  }
}
