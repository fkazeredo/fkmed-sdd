package com.fkmed.domain.reimbursement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A reimbursement request was submitted (SPEC-0015 §Events). Published by {@link
 * ReimbursementService#submit} inside the submission transaction; consumed by {@code
 * domain.notification} (SPEC-0004) to notify the requester's account. Carries only non-sensitive
 * fields — no provider/bank data.
 *
 * @param requestId the submitted request's id.
 * @param beneficiaryId the beneficiary the request was submitted for.
 * @param protocol the `RE-` protocol.
 * @param expenseType the expense-type code.
 * @param amount the amount paid.
 * @param expectedPaymentDate BR12's expected payment date.
 */
public record ReimbursementSubmitted(
    UUID requestId,
    UUID beneficiaryId,
    String protocol,
    String expenseType,
    BigDecimal amount,
    LocalDate expectedPaymentDate) {}
