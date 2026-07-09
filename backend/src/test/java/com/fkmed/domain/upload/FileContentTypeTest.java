package com.fkmed.domain.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FileContentTypeTest {

  @Test
  void detectsSupportedTypesFromExactMagicBytes() {
    assertThat(FileContentType.detect(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}))
        .contains(FileContentType.JPEG);
    assertThat(
            FileContentType.detect(
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}))
        .contains(FileContentType.PNG);
    assertThat(FileContentType.detect(new byte[] {0x25, 0x50, 0x44, 0x46}))
        .contains(FileContentType.PDF);
  }

  @Test
  void acceptsPayloadAfterTheMagicBytes() {
    assertThat(
            FileContentType.detect(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02}))
        .contains(FileContentType.JPEG);
    assertThat(FileContentType.detect(new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31}))
        .contains(FileContentType.PDF);
  }

  @Test
  void rejectsNullEmptyAndTruncatedContent() {
    assertThat(FileContentType.detect(null)).isEmpty();
    assertThat(FileContentType.detect(new byte[0])).isEmpty();
    assertThat(FileContentType.detect(new byte[] {(byte) 0xFF, (byte) 0xD8})).isEmpty();
    assertThat(FileContentType.detect(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A}))
        .isEmpty();
    assertThat(FileContentType.detect(new byte[] {0x25, 0x50, 0x44})).isEmpty();
  }

  @Test
  void rejectsEveryCorruptedMagicBytePosition() {
    List<byte[]> corrupted =
        List.of(
            new byte[] {0x00, (byte) 0xD8, (byte) 0xFF},
            new byte[] {(byte) 0xFF, 0x00, (byte) 0xFF},
            new byte[] {(byte) 0xFF, (byte) 0xD8, 0x00},
            new byte[] {0x00, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            new byte[] {(byte) 0x89, 0x00, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            new byte[] {(byte) 0x89, 0x50, 0x00, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            new byte[] {(byte) 0x89, 0x50, 0x4E, 0x00, 0x0D, 0x0A, 0x1A, 0x0A},
            new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x00, 0x0A, 0x1A, 0x0A},
            new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x00, 0x1A, 0x0A},
            new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x00, 0x0A},
            new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x00},
            new byte[] {0x00, 0x50, 0x44, 0x46},
            new byte[] {0x25, 0x00, 0x44, 0x46},
            new byte[] {0x25, 0x50, 0x00, 0x46},
            new byte[] {0x25, 0x50, 0x44, 0x00});

    assertThat(corrupted).allSatisfy(bytes -> assertThat(FileContentType.detect(bytes)).isEmpty());
  }
}
