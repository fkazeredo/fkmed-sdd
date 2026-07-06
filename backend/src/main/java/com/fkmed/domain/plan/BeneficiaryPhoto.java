package com.fkmed.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * A beneficiary's profile photo (SPEC-0006 BR2/BR3): the image bytes plus the content type sniffed
 * from those bytes. One per beneficiary; replacing it overwrites the row, "remover foto" deletes it
 * (back to the placeholder).
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

  @Column(nullable = false)
  private byte[] image;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** JPA only. */
  protected BeneficiaryPhoto() {}

  private BeneficiaryPhoto(
      UUID beneficiaryId, byte[] image, String contentType, Instant updatedAt) {
    this.beneficiaryId = beneficiaryId;
    this.image = image;
    this.contentType = contentType;
    this.updatedAt = updatedAt;
  }

  /**
   * Creates a beneficiary's photo from the uploaded bytes.
   *
   * @throws PhotoTooLargeException when the image exceeds 5 MB.
   * @throws PhotoInvalidContentException when the content is not a JPG or PNG.
   */
  static BeneficiaryPhoto of(UUID beneficiaryId, byte[] image, Instant updatedAt) {
    return new BeneficiaryPhoto(
        beneficiaryId, validated(image), ImageContent.detect(image), updatedAt);
  }

  /**
   * Replaces the image with newly uploaded bytes (same validation as creation), so re-uploading
   * updates the existing row rather than leaving a stale content type.
   */
  void replace(byte[] image, Instant updatedAt) {
    this.image = validated(image);
    this.contentType = ImageContent.detect(image);
    this.updatedAt = updatedAt;
  }

  private static byte[] validated(byte[] image) {
    if (image == null || image.length > MAX_BYTES) {
      throw new PhotoTooLargeException();
    }
    return image;
  }
}
