package com.fkmed.domain.reimbursement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Create/detail result for a reimbursement preview. */
public record ReimbursementPreviewResult(
    UUID id,
    String protocol,
    String expenseType,
    String beneficiary,
    PreviewSituation situation,
    BigDecimal estimatedValue,
    Instant createdAt,
    Instant concludedAt,
    String base,
    String disclaimer) {}
