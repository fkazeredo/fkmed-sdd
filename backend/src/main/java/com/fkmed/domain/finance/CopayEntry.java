package com.fkmed.domain.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * One copay charge for a usage by a family member (SPEC-0013 BR5). Operator-originated (BR8):
 * created only through {@link Copays#record}. Read-only over it, the titular sees the whole
 * family's entries filtered by period and beneficiary.
 */
@Entity
@Table(name = "copay_entry")
@Getter
public class CopayEntry {

  @Id private UUID id;

  @Column(name = "entry_date", nullable = false)
  private LocalDate entryDate;

  @Column(nullable = false)
  private String procedure;

  @Column(nullable = false)
  private String provider;

  @Column(name = "beneficiary_id", nullable = false)
  private UUID beneficiaryId;

  @Column(nullable = false)
  private BigDecimal amount;

  /** JPA only. */
  protected CopayEntry() {}

  /**
   * Records a new copay charge (the only construction path — BR8).
   *
   * @throws IllegalArgumentException when a required field is missing/blank or the amount is not
   *     positive — an internal-contract violation by the calling sim.
   */
  static CopayEntry record(
      LocalDate entryDate,
      String procedure,
      String provider,
      UUID beneficiaryId,
      BigDecimal amount) {
    Objects.requireNonNull(entryDate, "entryDate is required");
    Objects.requireNonNull(beneficiaryId, "beneficiaryId is required");
    Objects.requireNonNull(amount, "amount is required");
    requireText(procedure, "procedure");
    requireText(provider, "provider");
    if (amount.signum() <= 0) {
      throw new IllegalArgumentException("amount must be positive");
    }

    CopayEntry entry = new CopayEntry();
    entry.id = UUID.randomUUID();
    entry.entryDate = entryDate;
    entry.procedure = procedure;
    entry.provider = provider;
    entry.beneficiaryId = beneficiaryId;
    entry.amount = amount;
    return entry;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
  }
}
