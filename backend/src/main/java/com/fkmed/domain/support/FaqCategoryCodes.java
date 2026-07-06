package com.fkmed.domain.support;

import java.util.Set;

/**
 * The six fixed FAQ categories (SPEC-0014 BR5): Reembolso · Carteirinha · Agendamento ·
 * Telemedicina · Boletos · Rede.
 *
 * <p>Registry data, not an enum (DECISIONS-BASELINE §0019): the category vocabulary is
 * operator-editable content the FAQ seed carries (an operator adding a seventh category is a
 * content change, not a code change), so it is validated as a stable {@code String} code through
 * this {@code *Codes} constants holder rather than modelled as a persisted enum. The label is
 * rendered by the frontend/i18n; the stored value is the code.
 */
public final class FaqCategoryCodes {

  /** Reimbursement requests, tracking and preview (SPEC-0015/0016/0017). */
  public static final String REEMBOLSO = "REEMBOLSO";

  /** Digital card (SPEC-0007). */
  public static final String CARTEIRINHA = "CARTEIRINHA";

  /** Appointment scheduling (SPEC-0009). */
  public static final String AGENDAMENTO = "AGENDAMENTO";

  /** Telemedicine queue and sessions (SPEC-0010). */
  public static final String TELEMEDICINA = "TELEMEDICINA";

  /** Billing / invoices (SPEC-0013). */
  public static final String BOLETOS = "BOLETOS";

  /** Accredited provider network (SPEC-0008). */
  public static final String REDE = "REDE";

  private static final Set<String> ALL =
      Set.of(REEMBOLSO, CARTEIRINHA, AGENDAMENTO, TELEMEDICINA, BOLETOS, REDE);

  private FaqCategoryCodes() {}

  /** Whether {@code code} is one of the six fixed FAQ categories. */
  public static boolean isValid(String code) {
    return code != null && ALL.contains(code);
  }
}
