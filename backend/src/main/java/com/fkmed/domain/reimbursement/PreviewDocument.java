package com.fkmed.domain.reimbursement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/** Attachment for an analyzed reimbursement preview. */
@Entity
@Table(name = "preview_document")
@Getter
public class PreviewDocument {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "preview_id", nullable = false)
  private ReimbursementPreview preview;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DocumentCategory category;

  @Column(name = "storage_reference", nullable = false, length = 220)
  private String storageReference;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "file_size", nullable = false)
  private int fileSize;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt;

  protected PreviewDocument() {}

  private PreviewDocument(
      ReimbursementPreview preview,
      DocumentCategory category,
      String storageReference,
      String contentType,
      String fileName,
      int fileSize,
      Instant uploadedAt) {
    this.id = UUID.randomUUID();
    this.preview = preview;
    this.category = category;
    this.storageReference = storageReference;
    this.contentType = contentType;
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.uploadedAt = uploadedAt;
  }

  static PreviewDocument of(
      ReimbursementPreview preview,
      DocumentCategory category,
      String storageReference,
      String contentType,
      String fileName,
      int fileSize,
      Instant uploadedAt) {
    return new PreviewDocument(
        preview, category, storageReference, contentType, fileName, fileSize, uploadedAt);
  }
}
