package com.fkmed.domain.reimbursement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A bank registry entry (SPEC-0015 BR11) — public FEBRABAN reference codes, seed-only, read-only in
 * this phase.
 */
@Entity
@Table(name = "bank")
@Getter
public class Bank {

  @Id private String code;

  @Column(nullable = false, length = 80)
  private String name;

  /** JPA only. */
  protected Bank() {}
}
