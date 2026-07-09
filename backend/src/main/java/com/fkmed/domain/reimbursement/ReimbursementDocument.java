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

/**
 * An uploaded attachment (SPEC-0015 BR8) — content-validated (magic bytes, {@code
 * domain.upload.FileContentType}) and size-capped by {@link ReimbursementService} before this
 * entity is created; a child of {@link ReimbursementRequest}.
 */
@Entity
@Table(name = "reimbursement_document")
@Getter
public class ReimbursementDocument {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id", nullable = false)
  private ReimbursementRequest request;

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

  /** JPA only. */
  protected ReimbursementDocument() {}

  private ReimbursementDocument(
      ReimbursementRequest request,
      DocumentCategory category,
      String storageReference,
      String contentType,
      String fileName,
      int fileSize,
      Instant uploadedAt) {
    this.id = UUID.randomUUID();
    this.request = request;
    this.category = category;
    this.storageReference = storageReference;
    this.contentType = contentType;
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.uploadedAt = uploadedAt;
  }

  /** Creates a document bound to {@code request}; the caller has already validated its content. */
  static ReimbursementDocument of(
      ReimbursementRequest request,
      DocumentCategory category,
      String storageReference,
      String contentType,
      String fileName,
      int fileSize,
      Instant uploadedAt) {
    return new ReimbursementDocument(
        request, category, storageReference, contentType, fileName, fileSize, uploadedAt);
  }
}
