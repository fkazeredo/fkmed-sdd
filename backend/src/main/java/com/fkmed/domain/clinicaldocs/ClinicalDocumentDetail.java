package com.fkmed.domain.clinicaldocs;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The type-specific detail (SPEC-0011 BR6): the common header ({@link #type} through {@link
 * #expired}) plus only the fields {@link #type} populates — exam order: {@link #clinicalIndication}
 * + {@link #examItems}; referral: {@link #targetSpecialty} + {@link #reason}; prescription: {@link
 * #medications}; sick note: {@link #periodStart}/{@link #periodEnd}/{@link #cid}/{@link #notes}
 * (DL-0020: the CID IS displayed).
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
    List<ExamItemView> examItems,
    String targetSpecialty,
    String reason,
    List<PrescriptionItemView> medications,
    LocalDate periodStart,
    LocalDate periodEnd,
    String cid,
    String notes) {

  public record ExamItemView(String examName, String tussCode) {}

  public record PrescriptionItemView(String medication, String posology, String guidance) {}
}
