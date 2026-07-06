package com.fkmed.domain.clinicaldocs;

import java.time.LocalDate;

/**
 * A resolved, inclusive calendar-date range for the list filter (SPEC-0011 BR2: last 30/90/365 days
 * or a custom range). The HTTP-facing decision of which {@code period} code the client sent (and
 * validating a custom range's shape) belongs to the delivery layer ({@code
 * ClinicalDocumentController}, mirroring {@code AppointmentController#scope}); this value object
 * only guards the resulting range is well-formed.
 *
 * @param from the inclusive lower bound.
 * @param to the inclusive upper bound.
 */
public record DocumentPeriod(LocalDate from, LocalDate to) {

  public DocumentPeriod {
    if (from == null || to == null || from.isAfter(to)) {
      throw new IllegalArgumentException("'from' must not be after 'to'");
    }
  }

  /** Whether {@code issuedDate} falls within this range, both bounds inclusive. */
  boolean includes(LocalDate issuedDate) {
    return !issuedDate.isBefore(from) && !issuedDate.isAfter(to);
  }
}
