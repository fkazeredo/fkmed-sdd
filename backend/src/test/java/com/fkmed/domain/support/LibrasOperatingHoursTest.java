package com.fkmed.domain.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0014 BR4: the Central de Libras operating window — weekdays, 8h (inclusive) to 18h
 * (exclusive).
 */
class LibrasOperatingHoursTest {

  private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

  @Test
  void isOpenAt_onAWeekdayWithinTheWindow() {
    // Wednesday 2026-07-08, 10:00
    assertThat(LibrasOperatingHours.isOpenAt(ZonedDateTime.of(2026, 7, 8, 10, 0, 0, 0, ZONE)))
        .isTrue();
  }

  @Test
  void isOpenAt_exactlyAtOpeningBoundary_isOpen() {
    assertThat(LibrasOperatingHours.isOpenAt(ZonedDateTime.of(2026, 7, 8, 8, 0, 0, 0, ZONE)))
        .isTrue();
  }

  @Test
  void isOpenAt_exactlyAtClosingBoundary_isClosed() {
    // 18:00 is the exclusive upper bound.
    assertThat(LibrasOperatingHours.isOpenAt(ZonedDateTime.of(2026, 7, 8, 18, 0, 0, 0, ZONE)))
        .isFalse();
  }

  @Test
  void isOpenAt_beforeOpening_isClosed() {
    assertThat(LibrasOperatingHours.isOpenAt(ZonedDateTime.of(2026, 7, 8, 7, 59, 0, 0, ZONE)))
        .isFalse();
  }

  @Test
  void isOpenAt_onAWeekend_isAlwaysClosed() {
    // Saturday 2026-07-11, 10:00 — inside the weekday time window, but a weekend.
    assertThat(LibrasOperatingHours.isOpenAt(ZonedDateTime.of(2026, 7, 11, 10, 0, 0, 0, ZONE)))
        .isFalse();
    // Sunday 2026-07-12, 10:00
    assertThat(LibrasOperatingHours.isOpenAt(ZonedDateTime.of(2026, 7, 12, 10, 0, 0, 0, ZONE)))
        .isFalse();
  }
}
