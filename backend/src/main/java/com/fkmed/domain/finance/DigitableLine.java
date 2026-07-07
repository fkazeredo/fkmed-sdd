package com.fkmed.domain.finance;

/**
 * Boleto typeable-line utilities (SPEC-0013 BR3/BR4).
 *
 * <p>{@link #normalize} strips every non-digit so a line pasted with the usual dots/spaces matches
 * the stored canonical form (BR4 — normalize before any lookup). {@link #barcodeOf} derives the
 * 44-digit FEBRABAN barcode payload from the 47-digit typeable line by dropping the three field
 * check digits and reordering the fields — deterministic, used for the second-copy PDF's barcode
 * (BR3). No validity/DV verification is performed: the line's authenticity is decided by matching
 * an issued invoice (BR4), not by its own check digits.
 */
final class DigitableLine {

  static final int DIGITS = 47;

  private DigitableLine() {}

  /** Digits-only form of the input (removes spaces, dots and any other separators). */
  static String normalize(String raw) {
    return raw == null ? "" : raw.replaceAll("\\D", "");
  }

  /**
   * The 44-digit barcode payload for a 47-digit typeable line.
   *
   * @throws IllegalArgumentException when {@code line47} is not exactly 47 digits.
   */
  static String barcodeOf(String line47) {
    if (line47 == null || !line47.matches("\\d{47}")) {
      throw new IllegalArgumentException("a barcode is derivable only from a 47-digit line");
    }
    String field1 = line47.substring(0, 10); // 9 data digits + DV1
    String field2 = line47.substring(10, 21); // 10 data digits + DV2
    String field3 = line47.substring(21, 32); // 10 data digits + DV3
    String dvGeral = line47.substring(32, 33);
    String field5 = line47.substring(33, 47); // fator vencimento + valor
    return field1.substring(0, 4)
        + dvGeral
        + field5
        + field1.substring(4, 9)
        + field2.substring(0, 10)
        + field3.substring(0, 10);
  }
}
