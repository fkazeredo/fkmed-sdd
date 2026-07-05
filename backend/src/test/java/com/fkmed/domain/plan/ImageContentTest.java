package com.fkmed.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * SPEC-0006 BR2 magic-byte content sniffing: the file's leading bytes decide the type, never its
 * extension — an executable renamed {@code .png} is refused.
 */
class ImageContentTest {

  @Test
  void detectsJpegByMagicBytes() {
    byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
    assertThat(ImageContent.detect(jpeg)).isEqualTo("image/jpeg");
  }

  @Test
  void detectsPngByMagicBytes() {
    byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    assertThat(ImageContent.detect(png)).isEqualTo("image/png");
  }

  @Test
  void refusesAnExecutableRenamedPng() {
    byte[] pe = {0x4D, 0x5A, (byte) 0x90, 0x00, 0x03}; // "MZ" DOS/PE header
    assertThatThrownBy(() -> ImageContent.detect(pe))
        .isInstanceOf(PhotoInvalidContentException.class);
  }

  @Test
  void refusesTooShortContent() {
    assertThatThrownBy(() -> ImageContent.detect(new byte[] {(byte) 0xFF}))
        .isInstanceOf(PhotoInvalidContentException.class);
  }
}
