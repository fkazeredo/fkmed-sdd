package com.fkmed.domain.support;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * The Central de Libras operating window (SPEC-0014 BR4): weekdays, 8h to 18h — a fictitious
 * placeholder (SPEC-0014 OQ1, owner-approved 2026-07-06), the same convention as the V25 channel
 * seed, pending real operator-provided hours. Pure and Clock-driven (never the JVM/system clock
 * directly): the caller resolves "now" from the injected {@link java.time.Clock} in the product
 * timezone (America/Sao_Paulo, {@code infra.platform.TimeConfig}), so the boundary is fully
 * deterministic in tests.
 */
final class LibrasOperatingHours {

  /** The hours label shown to the beneficiary outside the operating window (BR4). */
  static final String HOURS_LABEL = "Segunda a sexta, das 8h às 18h";

  private static final LocalTime OPENS_AT = LocalTime.of(8, 0);
  private static final LocalTime CLOSES_AT = LocalTime.of(18, 0);

  private LibrasOperatingHours() {}

  /**
   * Whether {@code now} falls inside the operating window: Monday-to-Friday, 8h (inclusive) to 18h
   * (exclusive).
   */
  static boolean isOpenAt(ZonedDateTime now) {
    DayOfWeek day = now.getDayOfWeek();
    if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
      return false;
    }
    LocalTime time = now.toLocalTime();
    return !time.isBefore(OPENS_AT) && time.isBefore(CLOSES_AT);
  }
}
