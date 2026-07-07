package com.fkmed.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A new invoice was issued for a titular (SPEC-0013 §Events). Published by {@link Invoices#issue}
 * inside the issuance transaction; consumed by {@code domain.notification} (SPEC-0004) to notify
 * the titular. Carries only non-sensitive fields (competência, amount, due date) — the digitable
 * line and PIX code are NEVER put on the event (they must never be logged in full).
 *
 * @param invoiceId the issued invoice's id.
 * @param titularBeneficiaryId the contract titular the invoice belongs to.
 * @param competencia the reference month as "Mês/AAAA".
 * @param amount the original amount.
 * @param dueDate the due date.
 */
public record InvoiceIssued(
    UUID invoiceId,
    UUID titularBeneficiaryId,
    String competencia,
    BigDecimal amount,
    LocalDate dueDate) {}
