package com.fkmed.domain.appointment;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * The pure booking-window policy (SPEC-0009 BR5 + DL-0013): a slot is bookable only when its
 * calendar date falls within today..today+30 days and its start instant is at least the 2-hour
 * minimum antecedence ahead of now — evaluated in the product (clinic) timezone. Kept static and
 * side-effect-free so the timezone-sensitive rules are unit-testable without a database (backend.md
 * §Dates: timezone-sensitive rules MUST be tested).
 */
final class BookingHorizon {

  static final int HORIZON_DAYS = 30;
  static final Duration MIN_ANTECEDENCE = Duration.ofHours(2);

  private BookingHorizon() {}

  /** The real instant of a slot's local date+time in the given clinic zone. */
  static Instant instantOf(LocalDate date, LocalTime time, ZoneId zone) {
    return date.atTime(time).atZone(zone).toInstant();
  }

  /** Today in the clinic zone (the lower bound of the calendar horizon). */
  static LocalDate today(Instant now, ZoneId zone) {
    return LocalDate.ofInstant(now, zone);
  }

  /** The inclusive upper bound of the calendar horizon (today + 30 days). */
  static LocalDate horizonEnd(Instant now, ZoneId zone) {
    return today(now, zone).plusDays(HORIZON_DAYS);
  }

  /**
   * Whether a slot at {@code date}/{@code time} may be booked as of {@code now}: within the
   * calendar horizon and respecting the minimum antecedence. Rejects past dates, dates beyond
   * today+30 days and slots less than 2 hours ahead.
   */
  static boolean isBookable(LocalDate date, LocalTime time, Instant now, ZoneId zone) {
    LocalDate today = today(now, zone);
    if (date.isBefore(today) || date.isAfter(horizonEnd(now, zone))) {
      return false;
    }
    return !instantOf(date, time, zone).isBefore(now.plus(MIN_ANTECEDENCE));
  }
}
