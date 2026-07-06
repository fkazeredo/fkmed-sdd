package com.fkmed.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The antifraud validator's verdict for a submitted line (SPEC-0013 BR4). {@code AUTHENTIC} carries
 * the matched invoice's competência, due date and amount; {@code NOT_RECOGNIZED} carries nothing —
 * the validator MUST NEVER surface data that could suggest paying an unrecognized boleto.
 *
 * @param result {@code "AUTHENTIC"} or {@code "NOT_RECOGNIZED"}.
 * @param competencia the matched invoice's competência (only when AUTHENTIC).
 * @param dueDate the matched invoice's due date (only when AUTHENTIC).
 * @param amount the matched invoice's amount (only when AUTHENTIC).
 */
public record InvoiceValidation(
    String result, String competencia, LocalDate dueDate, BigDecimal amount) {

  static final String AUTHENTIC = "AUTHENTIC";
  static final String NOT_RECOGNIZED = "NOT_RECOGNIZED";

  static InvoiceValidation authentic(String competencia, LocalDate dueDate, BigDecimal amount) {
    return new InvoiceValidation(AUTHENTIC, competencia, dueDate, amount);
  }

  static InvoiceValidation notRecognized() {
    return new InvoiceValidation(NOT_RECOGNIZED, null, null, null);
  }
}
