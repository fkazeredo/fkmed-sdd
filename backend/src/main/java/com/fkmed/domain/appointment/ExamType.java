package com.fkmed.domain.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * An exam-catalog entry (SPEC-0009 BR4): the appointment module's reference-data registry (baseline
 * §0019) — a stable {@code code} validated on every exam booking, with a runtime-editable label.
 * Seeded by Flyway V16 (Hemograma, Raio-X, Ultrassonografia, Ressonância Magnética, Tomografia, …).
 */
@Entity
@Table(name = "exam_type")
@Getter
public class ExamType {

  @Id private String code;

  @Column(nullable = false)
  private String name;

  /** JPA only. */
  protected ExamType() {}
}
