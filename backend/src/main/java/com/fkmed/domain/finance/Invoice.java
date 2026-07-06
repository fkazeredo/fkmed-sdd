package com.fkmed.domain.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * A monthly invoice (boleto) issued to a contract's titular (SPEC-0013). Operator-originated (BR8):
 * created only through {@link Invoices#issue} — the portal never writes one. Immutable except for
 * the single business transition {@link #markPaid} (idempotent — BR6). The {@code digitableLine} is
 * stored in its canonical digits-only 47-char form (the validator normalizes input before matching,
 * BR4); {@code amount} is the ORIGINAL amount, on which the overdue update (multa + juros) is
 * computed at read time (BR2).
 */
@Entity
@Table(name = "invoice")
@Getter
public class Invoice {

  @Id private UUID id;

  @Column(name = "titular_beneficiary_id", nullable = false)
  private UUID titularBeneficiaryId;

  /** The reference month, stored as the first day of that month (BR2 "competência"). */
  @Column(nullable = false)
  private LocalDate competencia;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(name = "digitable_line", nullable = false, length = 47)
  private String digitableLine;

  @Column(name = "pix_code", nullable = false)
  private String pixCode;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** JPA only. */
  protected Invoice() {}

  /**
   * Issues a new invoice (the only construction path — BR8), unpaid, at {@code createdAt}. The
   * {@code digitableLine} MUST already be normalized to its 47-digit canonical form by the caller.
   *
   * @throws IllegalArgumentException when a required field is missing/blank or the digitable line
   *     is not exactly 47 digits — an internal-contract violation by the calling sim, never a
   *     client-facing error.
   */
  static Invoice issue(
      UUID titularBeneficiaryId,
      LocalDate competencia,
      LocalDate dueDate,
      BigDecimal amount,
      String digitableLine,
      String pixCode,
      Instant createdAt) {
    Objects.requireNonNull(titularBeneficiaryId, "titularBeneficiaryId is required");
    Objects.requireNonNull(competencia, "competencia is required");
    Objects.requireNonNull(dueDate, "dueDate is required");
    Objects.requireNonNull(amount, "amount is required");
    Objects.requireNonNull(createdAt, "createdAt is required");
    requireText(pixCode, "pixCode");
    if (digitableLine == null || !digitableLine.matches("\\d{47}")) {
      throw new IllegalArgumentException("digitableLine must be exactly 47 digits");
    }
    if (amount.signum() <= 0) {
      throw new IllegalArgumentException("amount must be positive");
    }

    Invoice invoice = new Invoice();
    invoice.id = UUID.randomUUID();
    invoice.titularBeneficiaryId = titularBeneficiaryId;
    invoice.competencia = competencia.withDayOfMonth(1);
    invoice.dueDate = dueDate;
    invoice.amount = amount;
    invoice.digitableLine = digitableLine;
    invoice.pixCode = pixCode;
    invoice.createdAt = createdAt;
    return invoice;
  }

  /**
   * Records the payment idempotently (BR6): the first call stamps {@code paidAt}; a repeat on an
   * already-paid invoice is a no-op (the original payment instant stays).
   *
   * @return {@code true} when this call transitioned the invoice to paid, {@code false} when it was
   *     already paid.
   */
  boolean markPaid(Instant at) {
    if (paidAt != null) {
      return false;
    }
    this.paidAt = at;
    return true;
  }

  /**
   * The derived status as of {@code today} (BR2): PAID when paid; OVERDUE when unpaid and past due.
   */
  public InvoiceStatus status(LocalDate today) {
    if (paidAt != null) {
      return InvoiceStatus.PAID;
    }
    return dueDate.isBefore(today) ? InvoiceStatus.OVERDUE : InvoiceStatus.OPEN;
  }

  public boolean paid() {
    return paidAt != null;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
  }
}
