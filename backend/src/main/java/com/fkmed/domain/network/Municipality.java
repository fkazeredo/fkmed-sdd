package com.fkmed.domain.network;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A Brazilian municipality in the seeded IBGE geography registry (DL-0014): the official IBGE code
 * as primary key, its name and the federative unit ({@code uf}, a plain 2-letter code — same
 * precedent as {@code domain.plan.ContactInfo.uf} — never a JPA relationship to {@code
 * uf_registry}, which is owned by another module). Seeded in full by Flyway V15 (~5,570 rows);
 * providers reference a real municipality (BR1/BR3), while {@code neighborhood} stays free text on
 * {@link Provider} since there is no authoritative national neighborhood dataset.
 */
@Entity
@Table(name = "municipality")
@Getter
public class Municipality {

  @Id
  @Column(name = "ibge_code")
  private Integer ibgeCode;

  @Column(nullable = false)
  private String name;

  @Column(length = 2, nullable = false)
  private String uf;

  /** JPA only. */
  protected Municipality() {}
}
