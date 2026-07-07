package com.fkmed.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A row of the invoice list (SPEC-0013 BR2). {@code paidAt} is present only for a PAID invoice;
 * {@code updatedAmount} (multa + juros over the original) only for an OVERDUE one — OPEN and PAID
 * carry only the original {@code amount}.
 *
 * @param id the invoice id.
 * @param competencia the reference month as "Mês/AAAA".
 * @param dueDate the due date.
 * @param amount the original amount.
 * @param status the derived status (OPEN / OVERDUE / PAID).
 * @param paidAt the payment date, when PAID (else null).
 * @param updatedAmount the overdue update, when OVERDUE (else null).
 */
public record InvoiceSummary(
    String id,
    String competencia,
    LocalDate dueDate,
    BigDecimal amount,
    InvoiceStatus status,
    LocalDate paidAt,
    UpdatedAmount updatedAmount) {}
