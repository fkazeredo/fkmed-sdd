package com.fkmed.domain.clinicaldocs;

/**
 * The four kinds of clinical document Minha Saúde manages (SPEC-0011 §Scope): exam order, referral,
 * prescription and sick note.
 *
 * <p>Kept as an enum, not registry data (invariant 7 / DECISIONS-BASELINE §0019): this is a closed,
 * four-value structural classification fixed by the product's document taxonomy — not
 * runtime-editable reference data — and it drives wired branching to type-specific fields, default
 * validity (BR4/DL-0019) and PDF layout. Same keep-criterion as {@code
 * domain.appointment.AppointmentType}.
 */
public enum ClinicalDocumentType {
  EXAM_ORDER,
  REFERRAL,
  PRESCRIPTION,
  SICK_NOTE
}
