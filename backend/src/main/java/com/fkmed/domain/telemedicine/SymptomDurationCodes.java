package com.fkmed.domain.telemedicine;

import java.util.Set;

/**
 * The fixed set of symptom-duration codes collected at triage (SPEC-0010 BR2: horas · 1–3 dias · +3
 * dias · +1 semana).
 *
 * <p>Not a registry table and not a business enum: the four buckets are fixed by the triage design
 * (no runtime editing, no per-code wired branching), so per DECISIONS-BASELINE §0019 they are
 * validated as stable {@code String} codes through this {@code *Codes} constants holder rather than
 * modelled as a persisted enum or a seeded registry. The label is rendered by the frontend/i18n;
 * the stored value is the code.
 */
public final class SymptomDurationCodes {

  /** A few hours. */
  public static final String HORAS = "HORAS";

  /** One to three days. */
  public static final String D1_3 = "D1_3";

  /** More than three days. */
  public static final String D3_MAIS = "D3_MAIS";

  /** More than a week. */
  public static final String SEMANA_MAIS = "SEMANA_MAIS";

  private static final Set<String> ALL = Set.of(HORAS, D1_3, D3_MAIS, SEMANA_MAIS);

  private SymptomDurationCodes() {}

  /** Whether {@code code} is one of the four fixed triage duration codes. */
  public static boolean isValid(String code) {
    return code != null && ALL.contains(code);
  }
}
