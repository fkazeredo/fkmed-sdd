package com.fkmed.domain.support;

import java.util.Set;

/**
 * The fixed set of FAQ categories (SPEC-0014 BR5: Reembolso · Carteirinha · Agendamento ·
 * Telemedicina · Boletos · Rede). Not a registry table and not a business enum: the six categories
 * are fixed by the product's information architecture (no runtime editing, no per-code wired
 * branching), so per DECISIONS-BASELINE §0019 they are validated as stable {@code String} codes
 * through this {@code *Codes} constants holder rather than a persisted enum or a seeded registry.
 * The label is rendered by the frontend/i18n; the stored value is the code.
 */
public final class FaqCategoryCodes {

  public static final String REEMBOLSO = "REEMBOLSO";
  public static final String CARTEIRINHA = "CARTEIRINHA";
  public static final String AGENDAMENTO = "AGENDAMENTO";
  public static final String TELEMEDICINA = "TELEMEDICINA";
  public static final String BOLETOS = "BOLETOS";
  public static final String REDE = "REDE";

  private static final Set<String> ALL =
      Set.of(REEMBOLSO, CARTEIRINHA, AGENDAMENTO, TELEMEDICINA, BOLETOS, REDE);

  private FaqCategoryCodes() {}

  /** Whether {@code code} is one of the six fixed FAQ categories. */
  public static boolean isValid(String code) {
    return code != null && ALL.contains(code);
  }
}
