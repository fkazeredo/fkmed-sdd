package com.fkmed.domain.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

/** SPEC-0013 BR3/BR4: line normalization and the 44-digit barcode derivation. */
class DigitableLineTest {

  private static final String LINE = "23793381286000826010494120780301189999000000001";

  @Test
  void normalize_stripsEverythingButDigits() {
    assertThat(DigitableLine.normalize("23793.38128 60008.260104 94120.780301 1 89999000000001"))
        .isEqualTo("23793381286000826010494120780301189999000000001");
  }

  @Test
  void normalize_ofNull_isEmpty() {
    assertThat(DigitableLine.normalize(null)).isEmpty();
  }

  @Test
  void normalize_ofAFormattedSeededLine_hasExactly47Digits() {
    String formatted = "23793.38128 60008.260104 94120.780301 1 89999000000001";
    assertThat(DigitableLine.normalize(formatted)).hasSize(DigitableLine.DIGITS);
  }

  @Test
  void barcodeOf_producesA44DigitPayload() {
    assertThat(DigitableLine.barcodeOf(LINE)).hasSize(44).matches("\\d{44}");
  }

  @Test
  void barcodeOf_reordersTheFieldsDroppingTheThreeFieldCheckDigits() {
    // field1=2379338128, field2=60008260104, field3=94120780301, dvGeral=1, field5=89999000000001
    // barcode = 2379 + 1 + 89999000000001 + 33812 + 6000826010 + 9412078030
    assertThat(DigitableLine.barcodeOf(LINE))
        .isEqualTo("23791" + "89999000000001" + "33812" + "6000826010" + "9412078030");
  }

  @Test
  void barcodeOf_rejectsANon47DigitLine() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> DigitableLine.barcodeOf("123"));
  }
}
