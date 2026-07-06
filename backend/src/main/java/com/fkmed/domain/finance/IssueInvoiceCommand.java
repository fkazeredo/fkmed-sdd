package com.fkmed.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The input to issue an invoice through {@link Invoices#issue} (SPEC-0013 §Operator-sim). The
 * {@code digitableLine} may arrive formatted (dots/spaces); it is normalized to its 47-digit
 * canonical form before persistence.
 */
public record IssueInvoiceCommand(
    UUID titularBeneficiaryId,
    LocalDate competencia,
    LocalDate dueDate,
    BigDecimal amount,
    String digitableLine,
    String pixCode) {}
