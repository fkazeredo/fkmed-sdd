package com.fkmed.domain.plan;

/**
 * Real image-content detection by magic bytes (SPEC-0006 BR2): the uploaded file's leading bytes,
 * never its extension or client-declared content type, decide whether it is a JPG or PNG. Anything
 * else is refused, so an executable renamed {@code .png} never passes.
 */
final class ImageContent {

  static final String JPEG = "image/jpeg";
  static final String PNG = "image/png";

  // JPEG: FF D8 FF. PNG: 89 50 4E 47 0D 0A 1A 0A.
  private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

  private ImageContent() {}

  /**
   * The detected content type ({@link #JPEG} or {@link #PNG}) of {@code bytes}.
   *
   * @throws PhotoInvalidContentException when the content is neither a JPG nor a PNG.
   */
  static String detect(byte[] bytes) {
    if (startsWith(bytes, JPEG_MAGIC)) {
      return JPEG;
    }
    if (startsWith(bytes, PNG_MAGIC)) {
      return PNG;
    }
    throw new PhotoInvalidContentException();
  }

  private static boolean startsWith(byte[] bytes, byte[] magic) {
    if (bytes == null || bytes.length < magic.length) {
      return false;
    }
    for (int i = 0; i < magic.length; i++) {
      if (bytes[i] != magic[i]) {
        return false;
      }
    }
    return true;
  }
}
