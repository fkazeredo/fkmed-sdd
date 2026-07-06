package com.fkmed.domain.clinicaldocs;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The type-specific detail (SPEC-0011 BR6): the common header ({@link #type} through {@link
 * #expired}) plus only the fields {@link #type} populates — exam order: {@link #clinicalIndication}
 * + {@link #exams}; referral: {@link #specialtyCode} + {@link #specialtyName} + {@link #reason};
 * prescription: {@link #medications}; sick note: {@link #periodStart}/{@link #periodEnd}/{@link
 * #cid}/{@link #notes} (DL-0020: the CID IS displayed).
 *
 * <p>The flat field names mirror the merged frontend's {@code document-detail.ts} exactly (Phase-4
 * Wave-2 reconciliation, minimizing FE churn): {@code exams[].name}/{@code .tuss}, the referral's
 * {@code specialtyCode} (handed to the SPEC-0009 wizard) with its snapshotted {@code
 * specialtyName}.
 */
public record ClinicalDocumentDetail(
    UUID id,
    ClinicalDocumentType type,
    LocalDate issuedAt,
    String professional,
    String crm,
    String beneficiary,
    LocalDate validUntil,
    boolean expired,
    String clinicalIndication,
    List<ExamItemView> exams,
    String specialtyCode,
    String specialtyName,
    String reason,
    List<PrescriptionItemView> medications,
    LocalDate periodStart,
    LocalDate periodEnd,
    String cid,
    String notes) {

  public record ExamItemView(String name, String tuss) {}

  public record PrescriptionItemView(String medication, String posology, String guidance) {}
}
