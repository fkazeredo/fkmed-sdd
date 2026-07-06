package com.fkmed.domain.clinicaldocs;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * A clinical document (SPEC-0011): the immutable common header (type, beneficiary, issuing
 * professional + CRM, issue date, stamped validity, origin) plus the type-specific fields — a
 * single flat table for the header and the single-valued type fields (referral, sick note), {@code
 * exam_order_item}/{@code prescription_item} child rows for the list-valued types (Rule Zero: four
 * narrow tables would over-model four constants' worth of extra fields).
 *
 * <p>Created ONLY through {@link #issue}, called by {@link ClinicalDocuments#issue} — no setters,
 * no update path (BR8 immutability): a correction issues a new document, the previous one stays in
 * history.
 */
@Entity
@Table(name = "clinical_document")
@Getter
public class ClinicalDocument {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ClinicalDocumentType type;

  @Column(name = "beneficiary_id", nullable = false)
  private UUID beneficiaryId;

  @Column(name = "professional_name", nullable = false)
  private String professionalName;

  @Column(nullable = false)
  private String crm;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "valid_until")
  private LocalDate validUntil;

  @Column(name = "origin_session_id")
  private UUID originSessionId;

  @Column(name = "origin_operator_id")
  private UUID originOperatorId;

  /** Exam order only (BR6). */
  @Column(name = "clinical_indication")
  private String clinicalIndication;

  /** Referral only (BR6): the registry specialty code the SPEC-0009 wizard pre-selects. */
  @Column(name = "target_specialty_code")
  private String targetSpecialtyCode;

  /** Referral only (BR6): the specialty's display name, snapshotted at issue (BR8 immutability). */
  @Column(name = "target_specialty_name")
  private String targetSpecialtyName;

  /** Referral only (BR6). */
  @Column(name = "referral_reason")
  private String referralReason;

  /** Sick note only (BR6). */
  @Column(name = "sick_note_period_start")
  private LocalDate sickNotePeriodStart;

  /** Sick note only (BR6). */
  @Column(name = "sick_note_period_end")
  private LocalDate sickNotePeriodEnd;

  /**
   * Sick note only (BR6, DL-0020: the CID IS displayed, overriding the spec's proposed default).
   */
  @Column private String cid;

  /** Sick note only (BR6). */
  @Column(name = "sick_note_notes")
  private String sickNoteNotes;

  /** Exam order only (BR6): the requested exams, in issuance order. */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "exam_order_item", joinColumns = @JoinColumn(name = "document_id"))
  @OrderColumn(name = "item_order")
  private List<ExamOrderItem> examItems = new ArrayList<>();

  /** Prescription only (BR6): the prescribed medications, in issuance order. */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "prescription_item", joinColumns = @JoinColumn(name = "document_id"))
  @OrderColumn(name = "item_order")
  private List<PrescriptionItem> prescriptionItems = new ArrayList<>();

  /** JPA only. */
  protected ClinicalDocument() {}

  /**
   * Issues a new document (the only construction path — BR8). Computes {@code valid_until} at issue
   * from the per-type default (BR4/DL-0019) and validates the fields {@link #type} requires.
   *
   * @throws IllegalArgumentException when a required common or type-specific field is missing — an
   *     internal-contract violation by the calling module (tele closure/operator sim), never a
   *     client-facing error.
   */
  static ClinicalDocument issue(
      IssueClinicalDocumentCommand command, Instant issuedAt, LocalDate issuedDate) {
    Objects.requireNonNull(command.type(), "type is required");
    Objects.requireNonNull(command.beneficiaryId(), "beneficiaryId is required");
    Objects.requireNonNull(command.origin(), "origin is required");
    requireText(command.professionalName(), "professionalName");
    requireText(command.crm(), "crm");

    ClinicalDocument document = new ClinicalDocument();
    document.id = UUID.randomUUID();
    document.type = command.type();
    document.beneficiaryId = command.beneficiaryId();
    document.professionalName = command.professionalName();
    document.crm = command.crm();
    document.issuedAt = issuedAt;
    document.validUntil = DocumentValidity.validUntilFor(command.type(), issuedDate);
    document.originSessionId = command.origin().sessionId();
    document.originOperatorId = command.origin().operatorAccountId();

    switch (command.type()) {
      case EXAM_ORDER -> document.applyExamOrder(command);
      case REFERRAL -> document.applyReferral(command);
      case PRESCRIPTION -> document.applyPrescription(command);
      case SICK_NOTE -> document.applySickNote(command);
      default -> throw new IllegalStateException("unknown document type: " + command.type());
    }
    return document;
  }

  /** Whether this document is expired as of {@code today} (BR5): read-time comparison, DL-0019. */
  public boolean expired(LocalDate today) {
    return DocumentValidity.isExpired(validUntil, today);
  }

  private void applyExamOrder(IssueClinicalDocumentCommand command) {
    requireText(command.clinicalIndication(), "clinicalIndication");
    if (command.examItems() == null || command.examItems().isEmpty()) {
      throw new IllegalArgumentException("an exam order requires at least one exam item");
    }
    this.clinicalIndication = command.clinicalIndication();
    this.examItems =
        command.examItems().stream()
            .map(item -> new ExamOrderItem(item.examName(), item.tussCode()))
            .toList();
  }

  private void applyReferral(IssueClinicalDocumentCommand command) {
    requireText(command.targetSpecialtyCode(), "targetSpecialtyCode");
    requireText(command.targetSpecialtyName(), "targetSpecialtyName");
    requireText(command.referralReason(), "referralReason");
    this.targetSpecialtyCode = command.targetSpecialtyCode();
    this.targetSpecialtyName = command.targetSpecialtyName();
    this.referralReason = command.referralReason();
  }

  private void applyPrescription(IssueClinicalDocumentCommand command) {
    if (command.medications() == null || command.medications().isEmpty()) {
      throw new IllegalArgumentException("a prescription requires at least one medication");
    }
    this.prescriptionItems =
        command.medications().stream()
            .map(item -> new PrescriptionItem(item.medication(), item.posology(), item.guidance()))
            .toList();
  }

  private void applySickNote(IssueClinicalDocumentCommand command) {
    Objects.requireNonNull(command.sickNotePeriodStart(), "sickNotePeriodStart is required");
    Objects.requireNonNull(command.sickNotePeriodEnd(), "sickNotePeriodEnd is required");
    if (command.sickNotePeriodEnd().isBefore(command.sickNotePeriodStart())) {
      throw new IllegalArgumentException(
          "sickNotePeriodEnd must not be before sickNotePeriodStart");
    }
    requireText(command.cid(), "cid");
    this.sickNotePeriodStart = command.sickNotePeriodStart();
    this.sickNotePeriodEnd = command.sickNotePeriodEnd();
    this.cid = command.cid();
    this.sickNoteNotes = command.sickNoteNotes();
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
  }
}
