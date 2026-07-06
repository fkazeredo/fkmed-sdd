package com.fkmed.domain.clinicaldocs;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

/**
 * One prescribed medication within a prescription document (SPEC-0011 BR6): medication, posology
 * and guidance. A pure value owned entirely by its {@link ClinicalDocument} (persisted in {@code
 * prescription_item}, no independent identity or repository — Rule Zero).
 */
@Embeddable
@Getter
public class PrescriptionItem {

  @Column(nullable = false)
  private String medication;

  @Column(nullable = false)
  private String posology;

  @Column private String guidance;

  /** JPA only. */
  protected PrescriptionItem() {}

  public PrescriptionItem(String medication, String posology, String guidance) {
    this.medication = medication;
    this.posology = posology;
    this.guidance = guidance;
  }
}
