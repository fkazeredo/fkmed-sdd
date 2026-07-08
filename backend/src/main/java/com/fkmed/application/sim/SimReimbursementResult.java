package com.fkmed.application.sim;

import com.fkmed.domain.reimbursement.ReimbursementStatus;
import java.time.LocalDate;
import java.util.UUID;

/** Operator-sim response for reimbursement actions. */
public record SimReimbursementResult(
    UUID id, String protocol, ReimbursementStatus status, LocalDate expectedPaymentDate) {}
