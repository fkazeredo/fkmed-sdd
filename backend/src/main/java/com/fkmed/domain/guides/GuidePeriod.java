package com.fkmed.domain.guides;

import java.time.LocalDate;

/**
 * The list filter's period codes (SPEC-0012 BR2): last 30/90 days or last 12 months.
 *
 * <p>Kept as an enum, not registry data (invariant 7 / DECISIONS-BASELINE §0019): a fixed,
 * request-shape classification bound directly at the API boundary (the frozen contract), not
 * runtime-editable reference data — mirrors {@code domain.clinicaldocs.DocumentPeriod}'s codes,
 * bound here as an enum rather than parsed strings because the filter carries no custom-range
 * variant.
 */
public enum GuidePeriod {
  LAST_30D,
  LAST_90D,
  LAST_12M;

  /** The inclusive lower bound of the period ending {@code today}. */
  public LocalDate from(LocalDate today) {
    return switch (this) {
      case LAST_30D -> today.minusDays(30);
      case LAST_90D -> today.minusDays(90);
      case LAST_12M -> today.minusMonths(12);
    };
  }
}
