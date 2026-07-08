package com.fkmed.domain.reimbursement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;

/**
 * The plan's reimbursement table (SPEC-0015 §Persistence, SPEC-0016 BR3): the amount reimbursed for
 * an expense type is {@code min(amount paid, this row's amount) x planMultiple}, per SESSION when
 * {@link #perSession} (Terapia/Psicologia) or per EVENT otherwise. {@code planMultiple} is fixed at
 * 1.0 in the POC (OQ2) but kept as a plan parameter for a future non-1.0 plan category. Seed-only,
 * read-only in this phase.
 */
@Entity
@Table(name = "reimbursement_table")
@Getter
public class ReimbursementTableEntry {

  @Id
  @Column(name = "expense_type_code")
  private String expenseTypeCode;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(name = "per_session", nullable = false)
  private boolean perSession;

  @Column(name = "plan_multiple", nullable = false)
  private BigDecimal planMultiple;

  /** JPA only. */
  protected ReimbursementTableEntry() {}
}
