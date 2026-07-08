package com.fkmed.domain.reimbursement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Domain event emitted for beneficiary-visible reimbursement transitions. */
public record ReimbursementStatusChanged(
    UUID requestId,
    UUID beneficiaryId,
    String protocol,
    ReimbursementStatus status,
    BigDecimal amountReimbursed,
    BigDecimal glosaAmount,
    String reason,
    String maskedBankAccount,
    Instant occurredAt) {}
