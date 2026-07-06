package com.fkmed.domain.appointment;

/**
 * Real content detection of the exam medical-order attachment by magic bytes (SPEC-0009 BR4,
 * DL-0015): the uploaded file's leading bytes — never its extension or client-declared content type
 * — decide whether it is a JPG, PNG or PDF, and the size is capped at 5 MB.
 *
 * <p>Deliberately duplicates the ~15-line detector of {@code domain.plan.ImageContent} (which is
 * package-private and cannot cross the module boundary) and adds the PDF signature ({@code %PDF} =
 * {@code 25 50 44 46}); promoting a shared detector is deferred until a third consumer appears
 * (DL-0015, Rule Zero).
 */
public record MedicalOrderContent(byte[] content, String contentType) {

  /** 5 MB (SPEC-0009 BR4). */
  static final int MAX_BYTES = 5 * 1024 * 1024;

  static final String JPEG = "image/jpeg";
  static final String PNG = "image/png";
  static final String PDF = "application/pdf";

  // JPEG: FF D8 FF. PNG: 89 50 4E 47 0D 0A 1A 0A. PDF: 25 50 44 46 (%PDF).
  private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

  /**
   * Validates the uploaded bytes and detects their content type.
   *
   * @throws MedicalOrderRequiredException when no bytes were supplied (the exam attachment is
   *     mandatory — BR4).
   * @throws MedicalOrderInvalidException when the content is not a JPG/PNG/PDF or exceeds 5 MB.
   */
  public static MedicalOrderContent of(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new MedicalOrderRequiredException();
    }
    if (bytes.length > MAX_BYTES) {
      throw new MedicalOrderInvalidException();
    }
    return new MedicalOrderContent(bytes, detect(bytes));
  }

  private static String detect(byte[] bytes) {
    if (startsWith(bytes, JPEG_MAGIC)) {
      return JPEG;
    }
    if (startsWith(bytes, PNG_MAGIC)) {
      return PNG;
    }
    if (startsWith(bytes, PDF_MAGIC)) {
      return PDF;
    }
    throw new MedicalOrderInvalidException();
  }

  private static boolean startsWith(byte[] bytes, byte[] magic) {
    if (bytes.length < magic.length) {
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
