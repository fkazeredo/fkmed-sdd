package com.fkmed.domain.telemedicine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A triage symptom-catalog entry (SPEC-0010 BR2): the telemedicine module's reference-data registry
 * (baseline §0019) — a stable {@code code} validated on every triage, with a runtime-editable
 * label. Seeded by Flyway V19.
 */
@Entity
@Table(name = "symptom")
@Getter
public class Symptom {

  @Id private String code;

  @Column(nullable = false)
  private String name;

  /**
   * Whether selecting this symptom is an emergency signal that raises the 24h-ER alert (SPEC-0010
   * BR3). Seeded by Flyway V21; read by the triage screen to drive the alert.
   */
  @Column(nullable = false)
  private boolean emergency;

  /** JPA only. */
  protected Symptom() {}
}
