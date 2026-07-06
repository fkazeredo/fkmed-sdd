package com.fkmed.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** SPEC-0009 BR4 / DL-0015: real content detection of the medical order by magic bytes + size. */
class MedicalOrderContentTest {

  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
  private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31}; // %PDF-1

  @Test
  void detects_jpeg_png_and_pdf_byContent() {
    assertThat(MedicalOrderContent.of(JPEG).contentType()).isEqualTo("image/jpeg");
    assertThat(MedicalOrderContent.of(PNG).contentType()).isEqualTo("image/png");
    assertThat(MedicalOrderContent.of(PDF).contentType()).isEqualTo("application/pdf");
  }

  @Test
  void rejects_missingAttachment_asRequired() {
    assertThatThrownBy(() -> MedicalOrderContent.of(null))
        .isInstanceOf(MedicalOrderRequiredException.class);
    assertThatThrownBy(() -> MedicalOrderContent.of(new byte[0]))
        .isInstanceOf(MedicalOrderRequiredException.class);
  }

  @Test
  void rejects_wrongContent_asInvalid() {
    byte[] executable = {0x4D, 0x5A, 0x00, 0x00}; // MZ (Windows executable), renamed .pdf
    assertThatThrownBy(() -> MedicalOrderContent.of(executable))
        .isInstanceOf(MedicalOrderInvalidException.class);
  }

  @Test
  void rejects_oversizeAttachment_asInvalid() {
    byte[] tooBig = new byte[MedicalOrderContent.MAX_BYTES + 1];
    tooBig[0] = (byte) 0xFF;
    tooBig[1] = (byte) 0xD8;
    tooBig[2] = (byte) 0xFF;
    assertThatThrownBy(() -> MedicalOrderContent.of(tooBig))
        .isInstanceOf(MedicalOrderInvalidException.class);
  }
}
