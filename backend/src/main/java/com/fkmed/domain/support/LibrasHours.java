package com.fkmed.domain.support;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The Central de Libras operating window (SPEC-0014 BR4, OQ1 owner-decided placeholder: Mon-Fri,
 * 08:00-18:00 America/Sao_Paulo) — swappable without a migration once the owner provides the
 * definitive hours; keep the {@code V25__support_channels_and_faq.sql} seed's Ouvidoria-style hours
 * copy in sync if this window changes. Deliberately a plain constants holder, not a Spring
 * {@code @ConfigurationProperties} binding: domain code must not depend on {@code infra}
 * (modules-and-apis.md), and a placeholder threshold does not warrant that machinery (Rule Zero).
 */
final class LibrasHours {

  static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  static final LocalTime START = LocalTime.of(8, 0);
  static final LocalTime END = LocalTime.of(18, 0);

  private LibrasHours() {}

  /** Whether {@code instant} falls inside the Mon-Fri {@link #START}-{@link #END} window. */
  static boolean isWithin(Instant instant) {
    ZonedDateTime zoned = instant.atZone(ZONE);
    DayOfWeek day = zoned.getDayOfWeek();
    if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
      return false;
    }
    LocalTime time = zoned.toLocalTime();
    return !time.isBefore(START) && time.isBefore(END);
  }
}
