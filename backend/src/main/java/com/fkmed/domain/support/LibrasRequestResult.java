package com.fkmed.domain.support;

import java.time.LocalTime;

/**
 * The outcome of registering a Libras service request (SPEC-0014 BR4/Input-Output Examples). {@code
 * nextStep} is {@code "videocall-shortly"} when registered inside the operating hours, or {@code
 * "next-period"} outside them — in which case {@code hoursStart}/{@code hoursEnd} carry the window
 * so the frontend can display it (never present on the inside-hours outcome).
 */
public record LibrasRequestResult(
    LibrasRequestSituation situation, String nextStep, LocalTime hoursStart, LocalTime hoursEnd) {

  static final String NEXT_STEP_VIDEOCALL_SHORTLY = "videocall-shortly";
  static final String NEXT_STEP_NEXT_PERIOD = "next-period";

  static LibrasRequestResult registered() {
    return new LibrasRequestResult(
        LibrasRequestSituation.REGISTERED, NEXT_STEP_VIDEOCALL_SHORTLY, null, null);
  }

  static LibrasRequestResult nextPeriod() {
    return new LibrasRequestResult(
        LibrasRequestSituation.REGISTERED,
        NEXT_STEP_NEXT_PERIOD,
        LibrasHours.START,
        LibrasHours.END);
  }
}
