package com.fkmed.domain.network;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A medical specialty — registry data, not an enum (SPEC-0008 BR6, baseline §0019): {@code code}
 * the stable identifier (referenced by {@link Provider#getSpecialtyCodes()} and, from SPEC-0009
 * onward, by {@code domain.appointment} through the {@link NetworkSpecialties} public facade — ADR-
 * 0011 Wave 2 freeze), {@code name} the editable pt-BR label listed alphabetically. Seeded by
 * Flyway V15 (≥ 15 rows); read-only at runtime in this phase.
 */
@Entity
@Table(name = "specialty")
@Getter
public class Specialty {

  @Id private String code;

  @Column(nullable = false)
  private String name;

  /** JPA only. */
  protected Specialty() {}
}
