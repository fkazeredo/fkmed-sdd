package com.fkmed.domain.support;

/**
 * Confirmation of a registered Libras service request (SPEC-0014 BR4).
 *
 * @param situation always {@link LibrasSituation#REGISTERED} in this slice.
 * @param nextStep {@code "videocall-shortly"} when the request lands inside the operating window
 *     ({@link LibrasOperatingHours}), else {@code "next-period"}.
 * @param hours the operating-hours label, present only when {@code nextStep} is {@code
 *     "next-period"} (BR4 — so the beneficiary knows when to expect the callback).
 */
public record LibrasRequestResponse(LibrasSituation situation, String nextStep, String hours) {

  static final String VIDEOCALL_SHORTLY = "videocall-shortly";
  static final String NEXT_PERIOD = "next-period";
}
