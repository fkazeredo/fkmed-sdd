package com.fkmed.domain.reimbursement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Paid-only statement payload (SPEC-0016 BR10). */
public record ReimbursementStatementView(List<StatementItem> items, BigDecimal total) {

  public record StatementItem(
      UUID id, String protocol, String beneficiary, Instant paidAt, BigDecimal amountPaid) {}
}
