package com.fkmed.domain.telemedicine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The fixed symptom-duration codes (SPEC-0010 BR2, §Validation Rules). */
class SymptomDurationCodesTest {

  @Test
  void theFourFixedCodes_areValid() {
    assertThat(SymptomDurationCodes.isValid(SymptomDurationCodes.HORAS)).isTrue();
    assertThat(SymptomDurationCodes.isValid(SymptomDurationCodes.D1_3)).isTrue();
    assertThat(SymptomDurationCodes.isValid(SymptomDurationCodes.D3_MAIS)).isTrue();
    assertThat(SymptomDurationCodes.isValid(SymptomDurationCodes.SEMANA_MAIS)).isTrue();
  }

  @Test
  void anythingElse_isInvalid() {
    assertThat(SymptomDurationCodes.isValid(null)).isFalse();
    assertThat(SymptomDurationCodes.isValid("")).isFalse();
    assertThat(SymptomDurationCodes.isValid("MESES")).isFalse();
    assertThat(SymptomDurationCodes.isValid("d1_3")).isFalse();
  }
}
