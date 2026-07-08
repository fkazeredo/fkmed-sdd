package com.fkmed.domain.reimbursement;

import java.util.Set;

/**
 * Stable expense-type codes (SPEC-0015 BR4) backed by the seeded {@code expense_type} registry
 * (DECISIONS-BASELINE §0019: a registry code — not a business enum — validated by {@link
 * ExpenseTypeRepository}, branching via these constants). Used where BR9's mandatory-document
 * matrix and the documentation guide genuinely branch per type; {@code per_session} (BR7) instead
 * comes from the {@code reimbursement_table} row, not from a code comparison.
 */
public final class ExpenseTypeCodes {

  public static final String CONSULTA = "CONSULTA";
  public static final String EXAME = "EXAME";
  public static final String TERAPIA = "TERAPIA";
  public static final String PSICOLOGIA = "PSICOLOGIA";
  public static final String HONORARIOS = "HONORARIOS";
  public static final String OUTROS = "OUTROS";

  /** BR8/BR9: every type except Consulta requires a pedido/relatório médico. */
  private static final Set<String> REQUIRE_MEDICAL_ORDER =
      Set.of(EXAME, TERAPIA, PSICOLOGIA, HONORARIOS, OUTROS);

  private ExpenseTypeCodes() {}

  /** Whether {@code code}'s mandatory-document matrix (BR8/BR9) requires a medical order/report. */
  public static boolean requiresMedicalOrder(String code) {
    return REQUIRE_MEDICAL_ORDER.contains(code);
  }
}
