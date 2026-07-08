package com.fkmed.domain.reimbursement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** List row for "Minhas previas" (SPEC-0017 BR7). */
public record ReimbursementPreviewListItem(
    UUID id,
    String protocol,
    String expenseType,
    String beneficiary,
    Instant requestedAt,
    PreviewSituation situation,
    BigDecimal estimatedValue) {}
