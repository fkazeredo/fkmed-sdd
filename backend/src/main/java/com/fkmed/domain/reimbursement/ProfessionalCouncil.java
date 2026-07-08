package com.fkmed.domain.reimbursement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** A professional-council registry entry (SPEC-0015 BR10) — seed-only, read-only in this phase. */
@Entity
@Table(name = "professional_council")
@Getter
public class ProfessionalCouncil {

  @Id private String code;

  @Column(nullable = false, length = 60)
  private String name;

  /** JPA only. */
  protected ProfessionalCouncil() {}
}
