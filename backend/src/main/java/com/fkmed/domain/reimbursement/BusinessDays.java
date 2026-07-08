package com.fkmed.domain.reimbursement;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Business-day arithmetic (SPEC-0015 BR12): Mon-Fri only (no public-holiday calendar in the POC —
 * out of scope). Used to compute the expected payment date (5 business days for Consulta, 10 for
 * every other expense type).
 */
final class BusinessDays {

  private BusinessDays() {}

  /** {@code start} plus {@code days} business days (Mon-Fri), skipping weekends. */
  static LocalDate plus(LocalDate start, int days) {
    LocalDate date = start;
    int added = 0;
    while (added < days) {
      date = date.plusDays(1);
      if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
        added++;
      }
    }
    return date;
  }
}
