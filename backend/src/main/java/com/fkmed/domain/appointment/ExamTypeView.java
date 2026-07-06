package com.fkmed.domain.appointment;

/**
 * An exam-catalog entry for the exam wizard (SPEC-0009 BR4), mirroring the network specialty
 * option.
 */
public record ExamTypeView(String code, String name) {

  static ExamTypeView from(ExamType exam) {
    return new ExamTypeView(exam.getCode(), exam.getName());
  }
}
