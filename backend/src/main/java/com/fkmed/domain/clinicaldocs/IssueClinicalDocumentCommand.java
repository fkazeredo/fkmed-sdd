package com.fkmed.domain.clinicaldocs;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The internal issuance facade's input (SPEC-0011: {@link ClinicalDocuments#issue}) — one flat
 * command carrying the common header plus every type's fields, only the ones matching {@link #type}
 * populated. Built through the per-type static factories below rather than the raw constructor, so
 * a Wave-2 caller (SPEC-0010 tele closure, SPEC-0018 operator sim) cannot accidentally mismatch the
 * type and its fields.
 */
public record IssueClinicalDocumentCommand(
    ClinicalDocumentType type,
    UUID beneficiaryId,
    String professionalName,
    String crm,
    DocumentOrigin origin,
    String clinicalIndication,
    List<ExamItemInput> examItems,
    String targetSpecialtyCode,
    String targetSpecialtyName,
    String referralReason,
    List<PrescriptionItemInput> medications,
    LocalDate sickNotePeriodStart,
    LocalDate sickNotePeriodEnd,
    String cid,
    String sickNoteNotes) {

  /** An exam-order document: the requested exams (name + TUSS) and the clinical indication. */
  public static IssueClinicalDocumentCommand examOrder(
      UUID beneficiaryId,
      String professionalName,
      String crm,
      DocumentOrigin origin,
      String clinicalIndication,
      List<ExamItemInput> examItems) {
    return new IssueClinicalDocumentCommand(
        ClinicalDocumentType.EXAM_ORDER,
        beneficiaryId,
        professionalName,
        crm,
        origin,
        clinicalIndication,
        examItems,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  /**
   * A referral document: the target specialty as its {@code domain.network} registry code (so the
   * SPEC-0009 wizard pre-selection matches an offered specialty) plus its display name snapshotted
   * at issue (BR8 immutability), and the reason.
   */
  public static IssueClinicalDocumentCommand referral(
      UUID beneficiaryId,
      String professionalName,
      String crm,
      DocumentOrigin origin,
      String targetSpecialtyCode,
      String targetSpecialtyName,
      String referralReason) {
    return new IssueClinicalDocumentCommand(
        ClinicalDocumentType.REFERRAL,
        beneficiaryId,
        professionalName,
        crm,
        origin,
        null,
        null,
        targetSpecialtyCode,
        targetSpecialtyName,
        referralReason,
        null,
        null,
        null,
        null,
        null);
  }

  /** A prescription document: the prescribed medications (medication, posology, guidance). */
  public static IssueClinicalDocumentCommand prescription(
      UUID beneficiaryId,
      String professionalName,
      String crm,
      DocumentOrigin origin,
      List<PrescriptionItemInput> medications) {
    return new IssueClinicalDocumentCommand(
        ClinicalDocumentType.PRESCRIPTION,
        beneficiaryId,
        professionalName,
        crm,
        origin,
        null,
        null,
        null,
        null,
        null,
        medications,
        null,
        null,
        null,
        null);
  }

  /**
   * A sick-note document: leave period, the CID (diagnosis code — DL-0020: displayed, not hidden)
   * and notes.
   */
  public static IssueClinicalDocumentCommand sickNote(
      UUID beneficiaryId,
      String professionalName,
      String crm,
      DocumentOrigin origin,
      LocalDate periodStart,
      LocalDate periodEnd,
      String cid,
      String notes) {
    return new IssueClinicalDocumentCommand(
        ClinicalDocumentType.SICK_NOTE,
        beneficiaryId,
        professionalName,
        crm,
        origin,
        null,
        null,
        null,
        null,
        null,
        null,
        periodStart,
        periodEnd,
        cid,
        notes);
  }
}
