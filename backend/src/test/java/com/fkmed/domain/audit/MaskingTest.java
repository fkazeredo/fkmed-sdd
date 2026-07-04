package com.fkmed.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SPEC-0003 BR8: personal-data masking for the audit trail. */
class MaskingTest {

  @Test
  void email_keepsFirstCharAndDomain() {
    assertThat(Masking.email("maria@fkmed.local")).isEqualTo("m***@fkmed.local");
  }

  @Test
  void email_blankOrNull_isEmpty() {
    assertThat(Masking.email("")).isEmpty();
    assertThat(Masking.email(null)).isEmpty();
  }

  @Test
  void email_withoutAt_isFullyMasked() {
    assertThat(Masking.email("noatsign")).isEqualTo("***");
  }

  @Test
  void email_withAtAtPositionZero_isFullyMasked() {
    // Boundary: `at <= 0` must catch position 0 too, not just "no @ at all" (indexOf == -1).
    assertThat(Masking.email("@fkmed.local")).isEqualTo("***");
  }

  @Test
  void cpf_revealsOnlyTheLastTwoDigits() {
    assertThat(Masking.cpf("52998224725")).isEqualTo("*********25");
  }

  @Test
  void cpf_blankOrNull_isEmpty() {
    assertThat(Masking.cpf(" ")).isEmpty();
    assertThat(Masking.cpf(null)).isEmpty();
  }

  @Test
  void cpf_ofExactlyTwoCharacters_isFullyMasked() {
    // Boundary: length() <= 2 — a non-blank 2-char value must not fall through to substring().
    assertThat(Masking.cpf("12")).isEqualTo("**");
  }

  @Test
  void cpf_ofThreeCharacters_revealsTheLastTwo() {
    assertThat(Masking.cpf("123")).isEqualTo("*23");
  }
}
