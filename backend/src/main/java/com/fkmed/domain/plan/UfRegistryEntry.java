package com.fkmed.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A federative unit in the seeded UF registry (baseline §0019): a stable 2-letter {@code code} and
 * an editable {@code name} label. Reference data, never an enum — the persisted contact UF is
 * validated against this registry (SPEC-0006 §Validation Rules).
 */
@Entity
@Table(name = "uf_registry")
@Getter
public class UfRegistryEntry {

  @Id
  @Column(length = 2)
  private String code;

  @Column(nullable = false)
  private String name;

  /** JPA only. */
  protected UfRegistryEntry() {}
}
