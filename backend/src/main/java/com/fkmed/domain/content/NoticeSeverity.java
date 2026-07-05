package com.fkmed.domain.content;

/**
 * Notice rendering severity (SPEC-0005 BR7).
 *
 * <p>Kept as an enum rather than registry data (invariant 7 / DECISIONS-BASELINE §0019): it is a
 * fixed <strong>technical classification of exactly two rendering styles</strong> the accordion
 * applies ({@code ALERT} visually distinct/urgent, {@code INFORMATIVE} neutral) — not an
 * operator-editable, growing business vocabulary.
 */
public enum NoticeSeverity {
  INFORMATIVE,
  ALERT
}
