package com.fkmed.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0009 BR5 + DL-0013: the booking window is today..+30 days with a 2-hour minimum antecedence,
 * evaluated in the clinic timezone (backend.md §Dates: timezone-sensitive rules MUST be tested).
 */
class BookingHorizonTest {

  private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  // 10:00 in São Paulo (UTC-3): today is 2026-07-06 in the clinic zone.
  private static final Instant NOW = Instant.parse("2026-07-06T13:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 7, 6);

  @Test
  void horizonBounds_areTodayAndTodayPlus30_inTheClinicZone() {
    assertThat(BookingHorizon.today(NOW, ZONE)).isEqualTo(TODAY);
    assertThat(BookingHorizon.horizonEnd(NOW, ZONE)).isEqualTo(TODAY.plusDays(30));
  }

  @Test
  void rejects_pastDate() {
    assertThat(BookingHorizon.isBookable(TODAY.minusDays(1), LocalTime.of(9, 0), NOW, ZONE))
        .isFalse();
  }

  @Test
  void rejects_slotLessThanTwoHoursAhead_butAcceptsExactlyTwoHours() {
    // 11:00 SP = 14:00Z, only 1h ahead of now (13:00Z) -> blocked.
    assertThat(BookingHorizon.isBookable(TODAY, LocalTime.of(11, 0), NOW, ZONE)).isFalse();
    // 12:00 SP = 15:00Z, exactly 2h ahead -> allowed.
    assertThat(BookingHorizon.isBookable(TODAY, LocalTime.of(12, 0), NOW, ZONE)).isTrue();
  }

  @Test
  void accepts_laterToday_tomorrow_andTheHorizonEdge() {
    assertThat(BookingHorizon.isBookable(TODAY, LocalTime.of(14, 0), NOW, ZONE)).isTrue();
    assertThat(BookingHorizon.isBookable(TODAY.plusDays(1), LocalTime.of(8, 0), NOW, ZONE))
        .isTrue();
    assertThat(BookingHorizon.isBookable(TODAY.plusDays(30), LocalTime.of(8, 0), NOW, ZONE))
        .isTrue();
  }

  @Test
  void rejects_beyondThirtyDayHorizon() {
    assertThat(BookingHorizon.isBookable(TODAY.plusDays(31), LocalTime.of(8, 0), NOW, ZONE))
        .isFalse();
  }

  @Test
  void instantOf_convertsClinicLocalDateTimeToUtcInstant() {
    assertThat(BookingHorizon.instantOf(TODAY, LocalTime.of(9, 0), ZONE))
        .isEqualTo(Instant.parse("2026-07-06T12:00:00Z"));
  }
}
