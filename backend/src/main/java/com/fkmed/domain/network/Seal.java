package com.fkmed.domain.network;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A provider qualification badge — registry data (SPEC-0008 BR14, DL-0012/OQ1): the official
 * meaning of these badges is unconfirmed by the product, so {@code code}/{@code name} identify the
 * badge and {@code description} is a parameterizable pt-BR label editable at runtime once the
 * product defines it, with no code change (BR12: shown on hover/touch). Seeded by Flyway V15;
 * read-only at runtime in this phase.
 */
@Entity
@Table(name = "seal")
@Getter
public class Seal {

  @Id private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String description;

  /** JPA only. */
  protected Seal() {}
}
