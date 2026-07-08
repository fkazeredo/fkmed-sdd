package com.fkmed.domain.upload;

import java.util.Optional;

/**
 * Real content detection of an uploaded file by its leading bytes — never its extension or
 * client-declared content type (DL-0027: the same small check as {@code
 * domain.appointment.MedicalOrderContent} and {@code domain.plan.ImageContent}, promoted here for
 * their third consumer, {@code domain.reimbursement} - ADR-0022). Detection only: each caller
 * enforces its own size limit and required/optional matrix with its own domain exceptions.
 */
public final class FileContentType {

  public static final String JPEG = "image/jpeg";
  public static final String PNG = "image/png";
  public static final String PDF = "application/pdf";

  // JPEG: FF D8 FF. PNG: 89 50 4E 47 0D 0A 1A 0A. PDF: 25 50 44 46 (%PDF).
  private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

  private FileContentType() {}

  /** The detected JPG/PNG/PDF content type from the leading bytes, or empty when unrecognized. */
  public static Optional<String> detect(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return Optional.empty();
    }
    if (startsWith(bytes, JPEG_MAGIC)) {
      return Optional.of(JPEG);
    }
    if (startsWith(bytes, PNG_MAGIC)) {
      return Optional.of(PNG);
    }
    if (startsWith(bytes, PDF_MAGIC)) {
      return Optional.of(PDF);
    }
    return Optional.empty();
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
