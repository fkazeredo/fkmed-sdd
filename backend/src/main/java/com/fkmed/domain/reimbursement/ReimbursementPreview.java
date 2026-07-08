package com.fkmed.domain.reimbursement;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

/** Non-binding reimbursement estimate (SPEC-0017). */
@Entity
@Table(name = "reimbursement_preview")
@Getter
public class ReimbursementPreview {

  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private String protocol;

  @Column(name = "beneficiary_id", nullable = false)
  private UUID beneficiaryId;

  @Column(name = "expense_type_code", nullable = false)
  private String expenseTypeCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PreviewSituation situation;

  @Column(name = "estimated_value")
  private BigDecimal estimatedValue;

  @Column(name = "concluded_at")
  private Instant concludedAt;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @OneToMany(
      mappedBy = "preview",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<PreviewDocument> documents = new ArrayList<>();

  protected ReimbursementPreview() {}

  static ReimbursementPreview immediate(
      String protocol,
      UUID beneficiaryId,
      String expenseTypeCode,
      BigDecimal estimatedValue,
      UUID createdBy,
      Instant now) {
    ReimbursementPreview preview = base(protocol, beneficiaryId, expenseTypeCode, createdBy, now);
    preview.situation = PreviewSituation.CONCLUIDA;
    preview.estimatedValue = estimatedValue;
    preview.concludedAt = now;
    return preview;
  }

  static ReimbursementPreview analyzed(
      String protocol,
      UUID beneficiaryId,
      String expenseTypeCode,
      List<UploadedDocument> documents,
      UUID createdBy,
      Instant now) {
    ReimbursementPreview preview = base(protocol, beneficiaryId, expenseTypeCode, createdBy, now);
    preview.situation = PreviewSituation.EM_ANALISE;
    for (UploadedDocument document : documents) {
      preview.documents.add(
          PreviewDocument.of(
              preview,
              document.category(),
              document.content(),
              document.contentType(),
              document.fileName(),
              now));
    }
    return preview;
  }

  void conclude(BigDecimal estimatedValue, Instant now) {
    if (situation == PreviewSituation.CONCLUIDA) {
      this.estimatedValue = estimatedValue;
      return;
    }
    this.situation = PreviewSituation.CONCLUIDA;
    this.estimatedValue = estimatedValue;
    this.concludedAt = now;
  }

  private static ReimbursementPreview base(
      String protocol, UUID beneficiaryId, String expenseTypeCode, UUID createdBy, Instant now) {
    ReimbursementPreview preview = new ReimbursementPreview();
    preview.id = UUID.randomUUID();
    preview.protocol = protocol;
    preview.beneficiaryId = beneficiaryId;
    preview.expenseTypeCode = expenseTypeCode;
    preview.createdBy = createdBy;
    preview.createdAt = now;
    return preview;
  }
}
