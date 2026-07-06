package com.fkmed.domain.appointment;

/**
 * Whether an appointment (and the agenda scope it books against) is a consultation or an exam
 * (SPEC-0009 §Scope).
 *
 * <p>Kept as an enum, not registry data (invariant 7 / DECISIONS-BASELINE §0019): this is a closed,
 * two-value structural classification fixed by the product's two booking flows — not runtime-
 * editable reference data — and it drives wired branching (a consultation resolves a specialty
 * through the network registry, an exam resolves an exam-type code and requires a medical-order
 * attachment). Same keep-criterion as {@code domain.plan.BeneficiaryRole}.
 */
public enum AppointmentType {
  CONSULTATION,
  EXAM
}
