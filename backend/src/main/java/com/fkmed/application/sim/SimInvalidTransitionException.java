package com.fkmed.application.sim;

import org.springframework.http.HttpStatus;

/**
 * The requested operator-simulation action is not allowed by the owning module's state machine
 * (SPEC-0018 BR4): e.g. closing a session that is not being attended. Rejected with 409 {@code
 * sim.invalid-transition}; the state is never forced. Translates the owning module's guard (an
 * {@link IllegalStateException} from the tele state machine) into the sim's stable contract.
 */
public class SimInvalidTransitionException extends SimException {

  public static final String CODE = "sim.invalid-transition";

  public SimInvalidTransitionException() {
    super(CODE, HttpStatus.CONFLICT);
  }
}
