package com.fkmed.domain.reimbursement;

import java.math.BigDecimal;

/** Result of the SPEC-0016 BR3 reimbursement calculation. */
public record ReimbursementCalculation(
    BigDecimal amountReimbursed, BigDecimal glosaAmount, String glosaReason) {

  static final String TABLE_LIMIT_REASON = "Valor excede a tabela do plano";

  static ReimbursementCalculation of(BigDecimal reimbursed, BigDecimal requested) {
    BigDecimal glosa = requested.subtract(reimbursed);
    if (glosa.signum() <= 0) {
      return new ReimbursementCalculation(reimbursed, BigDecimal.ZERO, null);
    }
    return new ReimbursementCalculation(reimbursed, glosa, TABLE_LIMIT_REASON);
  }
}
