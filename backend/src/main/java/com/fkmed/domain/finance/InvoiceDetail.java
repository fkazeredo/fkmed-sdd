package com.fkmed.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The invoice detail (SPEC-0013 BR3): the list summary plus the payment identifiers — the 47-digit
 * digitable line, the PIX copia-e-cola code and the derived barcode payload. Exposure only (no
 * online payment — BR8).
 */
public record InvoiceDetail(
    String id,
    String competencia,
    LocalDate dueDate,
    BigDecimal amount,
    InvoiceStatus status,
    LocalDate paidAt,
    UpdatedAmount updatedAmount,
    String digitableLine,
    String pixCode,
    String barcodePayload) {}
