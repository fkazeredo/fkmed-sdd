package com.fkmed.application.sim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Body of {@code POST /api/sim/finance/invoices} (SPEC-0013 §Operator-sim): issues a new OPEN
 * invoice for a contract titular. {@code competencia} is the reference month as {@code "YYYY-MM"};
 * {@code digitableLine} may arrive formatted (it is normalized to 47 digits before persistence).
 */
public record SimCreateInvoiceRequest(
    @NotNull UUID titularBeneficiaryId,
    @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String competencia,
    @NotNull LocalDate dueDate,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String digitableLine,
    @NotBlank String pixCode) {

  /** The competência parsed to the first day of its month. */
  public LocalDate competenciaDate() {
    return LocalDate.parse(competencia + "-01");
  }
}
