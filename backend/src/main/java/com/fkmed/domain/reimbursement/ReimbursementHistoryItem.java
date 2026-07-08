package com.fkmed.domain.reimbursement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Card/list row for SPEC-0016 history. */
public record ReimbursementHistoryItem(
    UUID id,
    String protocol,
    String expenseType,
    String beneficiary,
    Instant requestedAt,
    BigDecimal amountRequested,
    BigDecimal amountReimbursed,
    ReimbursementStatus status) {}
