package com.fkmed.application.sim;

import org.springframework.http.HttpStatus;

/**
 * The caller is authenticated but is not an internal operator (SPEC-0018 BR2): a beneficiary
 * account reaching any {@code /api/sim/**} route is rejected with 403 {@code sim.forbidden}. Only a
 * seeded OPERATOR_SIM credential (config allowlist) may drive the operator simulation.
 */
public class SimForbiddenException extends SimException {

  public static final String CODE = "sim.forbidden";

  public SimForbiddenException() {
    super(CODE, HttpStatus.FORBIDDEN);
  }
}
