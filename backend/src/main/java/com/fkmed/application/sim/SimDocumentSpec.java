package com.fkmed.application.sim;

import com.fkmed.domain.clinicaldocs.ClinicalDocumentType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * The type-specific payload of a document the operator simulation issues (SPEC-0018 BR5 / SPEC-0011
 * BR6), only the fields matching {@link #type} populated. Its field names mirror the merged
 * frontend's clinical-document detail shape ({@code exams[].name/.tuss}, {@code
 * medications[].medication/.posology/.guidance}, {@code specialtyCode}) so a demo/E2E can issue
 * what Minha Saúde then renders. The referral's {@code specialtyName} is NOT supplied here — the
 * sim resolves it from the {@code domain.network} registry so it always matches an offered
 * specialty.
 */
public record SimDocumentSpec(
    @NotNull ClinicalDocumentType type,
    String clinicalIndication,
    List<ExamItem> exams,
    String specialtyCode,
    String reason,
    List<Medication> medications,
    LocalDate periodStart,
    LocalDate periodEnd,
    String cid,
    String notes) {

  /** A requested exam of an EXAM_ORDER (name + TUSS code). */
  public record ExamItem(String name, String tuss) {}

  /** A prescribed medication of a PRESCRIPTION (medication + posology + guidance). */
  public record Medication(String medication, String posology, String guidance) {}
}
