package com.fkmed.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * A beneficiary's profile photo (SPEC-0006 BR2/BR3): an opaque storage reference plus the content
 * type sniffed from the uploaded bytes. One per beneficiary; replacing it updates the reference,
 * while "remover foto" deletes it (back to the placeholder).
 *
 * <p>Invariants: the content is a real JPG or PNG (magic bytes, never the extension) and the size
 * is at most 5 MB — both enforced on every create/replace.
 */
@Entity
@Table(name = "beneficiary_photo")
@Getter
public class BeneficiaryPhoto {

  /** 5 MB (SPEC-0006 BR2). */
  static final int MAX_BYTES = 5 * 1024 * 1024;

  @Id
  @Column(name = "beneficiary_id")
  private UUID beneficiaryId;

  @Column(name = "storage_reference", nullable = false, length = 220)
  private String storageReference;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** JPA only. */
  protected BeneficiaryPhoto() {}

  private BeneficiaryPhoto(
      UUID beneficiaryId, String storageReference, String contentType, Instant updatedAt) {
    this.beneficiaryId = beneficiaryId;
    this.storageReference = storageReference;
    this.contentType = contentType;
    this.updatedAt = updatedAt;
  }

  /**
   * Creates a beneficiary's photo from the uploaded bytes.
   *
   * @throws PhotoTooLargeException when the image exceeds 5 MB.
   * @throws PhotoInvalidContentException when the content is not a JPG or PNG.
   */
  static BeneficiaryPhoto of(
      UUID beneficiaryId, byte[] image, String storageReference, Instant updatedAt) {
    return new BeneficiaryPhoto(
        beneficiaryId,
        Objects.requireNonNull(storageReference, "storageReference"),
        validateUpload(image),
        updatedAt);
  }

  /**
   * Replaces the image with newly uploaded bytes (same validation as creation), so re-uploading
   * updates the existing row rather than leaving a stale content type.
   */
  void replace(byte[] image, String storageReference, Instant updatedAt) {
    this.storageReference = Objects.requireNonNull(storageReference, "storageReference");
    this.contentType = validateUpload(image);
    this.updatedAt = updatedAt;
  }

  static String validateUpload(byte[] image) {
    if (image == null || image.length > MAX_BYTES) {
      throw new PhotoTooLargeException();
    }
    return ImageContent.detect(image);
  }
}
