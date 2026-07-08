package com.fkmed.domain.reimbursement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** An expense-type registry entry (SPEC-0015 BR4) — seed-only, read-only in this phase. */
@Entity
@Table(name = "expense_type")
@Getter
public class ExpenseType {

  @Id private String code;

  @Column(nullable = false, length = 60)
  private String name;

  /** JPA only. */
  protected ExpenseType() {}
}
