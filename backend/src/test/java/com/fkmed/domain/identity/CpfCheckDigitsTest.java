package com.fkmed.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * SPEC-0002 §Validation Rules ("CPF: 11 digits, valid check digits") — the mod-11 algorithm, with
 * known valid/invalid CPFs including check-digit-only-wrong cases (a value with correct length and
 * format but a tampered check digit must still be rejected).
 */
class CpfCheckDigitsTest {

  // Known-valid CPFs already seeded/used elsewhere in this codebase (V1 migration, test fixtures).
  private static final String MARIA_CPF = "52998224725";
  private static final String PEDRO_CPF = "15350946056";
  private static final String COMMON_TEST_CPF = "12345678909";

  @Test
  void isValid_acceptsKnownValidCpfs() {
    assertThat(CpfCheckDigits.isValid(MARIA_CPF)).isTrue();
    assertThat(CpfCheckDigits.isValid(PEDRO_CPF)).isTrue();
    assertThat(CpfCheckDigits.isValid(COMMON_TEST_CPF)).isTrue();
  }

  @Test
  void isValid_rejectsATamperedFirstCheckDigit() {
    // MARIA's CPF with only the first check digit wrong (same fixture as BeneficiaryTest).
    assertThat(CpfCheckDigits.isValid("52998224715")).isFalse();
  }

  @Test
  void isValid_rejectsATamperedSecondCheckDigit() {
    // MARIA's CPF with only the second check digit wrong (same fixture as BeneficiaryTest).
    assertThat(CpfCheckDigits.isValid("52998224724")).isFalse();
  }

  @Test
  void isValid_rejectsAllRepeatedDigits() {
    assertThat(CpfCheckDigits.isValid("11111111111")).isFalse();
    assertThat(CpfCheckDigits.isValid("00000000000")).isFalse();
  }

  @Test
  void isValid_rejectsWrongLength() {
    assertThat(CpfCheckDigits.isValid("1234567890")).isFalse();
    assertThat(CpfCheckDigits.isValid("123456789012")).isFalse();
  }

  @Test
  void isValid_rejectsNonDigits() {
    assertThat(CpfCheckDigits.isValid("5299822472a")).isFalse();
  }

  @Test
  void isValid_rejectsNull() {
    assertThat(CpfCheckDigits.isValid(null)).isFalse();
  }

  @Test
  void isValid_rejectsBlank() {
    assertThat(CpfCheckDigits.isValid("")).isFalse();
  }
}
