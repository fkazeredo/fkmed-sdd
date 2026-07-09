package com.fkmed.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0006 BR2: the photo entity enforces real content type and the 5 MB size cap. */
class BeneficiaryPhotoTest {

  private static final UUID ID = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-07-05T10:00:00Z");

  private static byte[] pngOf(int size) {
    byte[] bytes = new byte[size];
    byte[] magic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(magic, 0, bytes, 0, magic.length);
    return bytes;
  }

  @Test
  void storesTheSniffedContentType() {
    BeneficiaryPhoto photo = BeneficiaryPhoto.of(ID, pngOf(64), "postgres:photo-1", NOW);
    assertThat(photo.getContentType()).isEqualTo("image/png");
    assertThat(photo.getBeneficiaryId()).isEqualTo(ID);
    assertThat(photo.getStorageReference()).isEqualTo("postgres:photo-1");
  }

  @Test
  void refusesContentOverFiveMegabytes() {
    byte[] tooBig = pngOf(5 * 1024 * 1024 + 1);
    assertThatThrownBy(() -> BeneficiaryPhoto.of(ID, tooBig, "postgres:photo-1", NOW))
        .isInstanceOf(PhotoTooLargeException.class);
  }

  @Test
  void refusesNonImageContent() {
    byte[] notImage = {0x25, 0x50, 0x44, 0x46}; // "%PDF"
    assertThatThrownBy(() -> BeneficiaryPhoto.of(ID, notImage, "postgres:photo-1", NOW))
        .isInstanceOf(PhotoInvalidContentException.class);
  }

  @Test
  void replaceUpdatesReferenceAndContentType() {
    BeneficiaryPhoto photo = BeneficiaryPhoto.of(ID, pngOf(64), "postgres:photo-1", NOW);
    byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    photo.replace(jpeg, "filesystem:photo-2", NOW.plusSeconds(60));

    assertThat(photo.getContentType()).isEqualTo("image/jpeg");
    assertThat(photo.getStorageReference()).isEqualTo("filesystem:photo-2");
  }
}
