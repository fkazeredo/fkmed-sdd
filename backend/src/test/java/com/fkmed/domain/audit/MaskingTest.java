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
  void cpf_revealsOnlyTheLastTwoDigits() {
    assertThat(Masking.cpf("52998224725")).isEqualTo("*********25");
  }

  @Test
  void cpf_blankOrNull_isEmpty() {
    assertThat(Masking.cpf(" ")).isEmpty();
    assertThat(Masking.cpf(null)).isEmpty();
  }
}
