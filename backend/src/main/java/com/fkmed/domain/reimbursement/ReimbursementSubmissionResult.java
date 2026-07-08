package com.fkmed.domain.reimbursement;

import java.time.LocalDate;

/**
 * The outcome of a successful submission (SPEC-0015 BR12/BR13): the protocol, initial status and
 * expected payment date, plus the note about the ANS regulatory ceiling (BR12).
 */
public record ReimbursementSubmissionResult(
    String protocol, ReimbursementStatus status, LocalDate expectedPaymentDate) {}
