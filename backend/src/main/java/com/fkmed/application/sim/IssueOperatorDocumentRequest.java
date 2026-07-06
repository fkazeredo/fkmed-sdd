package com.fkmed.application.sim;

import com.fkmed.domain.clinicaldocs.ClinicalDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Body of {@code POST /api/sim/documents} (SPEC-0018 BR5): an operator-issued document for a
 * beneficiary (origin = operator, not a session). Flat on the wire ({@code type} + the
 * type-specific fields at the top level, mirroring the FE detail shape); {@link #spec()} projects
 * the type-specific fields onto the shared {@link SimDocumentSpec} so both issuance paths reuse the
 * same mapping.
 */
public record IssueOperatorDocumentRequest(
    @NotNull UUID beneficiaryId,
    @NotBlank String professionalName,
    @NotBlank String crm,
    @NotNull ClinicalDocumentType type,
    String clinicalIndication,
    List<SimDocumentSpec.ExamItem> exams,
    String specialtyCode,
    String reason,
    List<SimDocumentSpec.Medication> medications,
    LocalDate periodStart,
    LocalDate periodEnd,
    String cid,
    String notes) {

  /** The type-specific payload as the shared spec the sim maps to an issuance command. */
  public SimDocumentSpec spec() {
    return new SimDocumentSpec(
        type,
        clinicalIndication,
        exams,
        specialtyCode,
        reason,
        medications,
        periodStart,
        periodEnd,
        cid,
        notes);
  }
}
