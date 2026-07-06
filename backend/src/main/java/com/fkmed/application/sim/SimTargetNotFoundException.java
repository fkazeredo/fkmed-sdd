package com.fkmed.application.sim;

import org.springframework.http.HttpStatus;

/**
 * The operator-simulation target does not exist (SPEC-0018 §Error Behavior): e.g. no queued session
 * to attend or an unknown session id. Rejected with 404 {@code sim.target-not-found}.
 */
public class SimTargetNotFoundException extends SimException {

  public static final String CODE = "sim.target-not-found";

  public SimTargetNotFoundException() {
    super(CODE, HttpStatus.NOT_FOUND);
  }
}
