package com.fkmed.domain.clinicaldocs;

import java.time.LocalDate;

/**
 * The pure per-type validity policy (SPEC-0011 BR4/DL-0019): prescription 30 days, exam order 90
 * days and referral 90 days from issue; sick note carries no validity. {@code valid_until} is
 * computed once at issue time from these constants and persisted on the immutable document — a
 * later change to these constants never mutates an already-issued document. Kept static and
 * side-effect-free so the boundary-day rule is unit-testable without a database (backend.md §Dates:
 * timezone-sensitive/date rules MUST be tested).
 */
final class DocumentValidity {

  static final int PRESCRIPTION_DAYS = 30;
  static final int EXAM_ORDER_DAYS = 90;
  static final int REFERRAL_DAYS = 90;

  private DocumentValidity() {}

  /** The {@code valid_until} stamped at issue for {@code type}, or {@code null} for a sick note. */
  static LocalDate validUntilFor(ClinicalDocumentType type, LocalDate issuedDate) {
    return switch (type) {
      case PRESCRIPTION -> issuedDate.plusDays(PRESCRIPTION_DAYS);
      case EXAM_ORDER -> issuedDate.plusDays(EXAM_ORDER_DAYS);
      case REFERRAL -> issuedDate.plusDays(REFERRAL_DAYS);
      case SICK_NOTE -> null;
    };
  }

  /**
   * Whether a document is expired as of {@code today} (BR5): a document with no validity (sick
   * note) never expires; otherwise expired the day AFTER {@code validUntil} — the document stays
   * "Válido até" (valid through) its stamped date, inclusive.
   */
  static boolean isExpired(LocalDate validUntil, LocalDate today) {
    return validUntil != null && today.isAfter(validUntil);
  }
}
