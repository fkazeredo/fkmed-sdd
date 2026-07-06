package com.fkmed.domain.guides;

/**
 * The three TISS guide kinds Minha Guias tracks (SPEC-0012 BR2): consultation, SP/SADT (exams and
 * other procedures) and hospitalization.
 *
 * <p>Kept as an enum, not registry data (invariant 7 / DECISIONS-BASELINE §0019): this is a closed,
 * three-value structural classification fixed by the TISS guide taxonomy — not runtime-editable
 * reference data — mirroring {@code domain.clinicaldocs.ClinicalDocumentType}'s keep criterion.
 */
public enum GuideType {
  CONSULTA,
  SP_SADT,
  INTERNACAO
}
