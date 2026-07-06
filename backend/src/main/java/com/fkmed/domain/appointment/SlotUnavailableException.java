package com.fkmed.domain.appointment;

import com.fkmed.domain.error.DomainException;

/**
 * The chosen slot has no free seat at confirmation time (SPEC-0009 BR6/AC3) — either it was already
 * full or a concurrent confirmation took the last seat first. Fail-fast: the loser of the race is
 * told immediately, never retried (ADR-0012). Maps to {@code 409 appointment.slot-taken}.
 */
public class SlotUnavailableException extends DomainException {

  public static final String CODE = "appointment.slot-taken";

  public SlotUnavailableException() {
    super(CODE);
  }
}
