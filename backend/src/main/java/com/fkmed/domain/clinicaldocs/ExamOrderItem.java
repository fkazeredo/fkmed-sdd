package com.fkmed.domain.clinicaldocs;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

/**
 * One requested exam within an exam-order document (SPEC-0011 BR6): the exam name and its TUSS
 * code. A pure value owned entirely by its {@link ClinicalDocument} (persisted in {@code
 * exam_order_item}, no independent identity or repository — Rule Zero).
 */
@Embeddable
@Getter
public class ExamOrderItem {

  @Column(name = "exam_name", nullable = false)
  private String examName;

  @Column(name = "tuss_code", nullable = false)
  private String tussCode;

  /** JPA only. */
  protected ExamOrderItem() {}

  public ExamOrderItem(String examName, String tussCode) {
    this.examName = examName;
    this.tussCode = tussCode;
  }
}
