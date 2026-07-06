package com.fkmed.domain.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SPEC-0014 BR5: case/accent-insensitive substring matching for the FAQ search. */
class NormalizedTextTest {

  @Test
  void matchesRegardlessOfCase() {
    assertThat(NormalizedText.contains("Reembolso", "reembolso")).isTrue();
    assertThat(NormalizedText.contains("Reembolso", "REEMBOLSO")).isTrue();
  }

  @Test
  void matchesRegardlessOfAccents() {
    assertThat(NormalizedText.contains("Carteirinha", "carteirinha")).isTrue();
    assertThat(NormalizedText.contains("Não representa o valor final", "nao representa")).isTrue();
  }

  @Test
  void matchesAsSubstring_anyPosition() {
    assertThat(NormalizedText.contains("Como solicitar reembolso de despesas", "reembolso"))
        .isTrue();
  }

  @Test
  void blankOrNullNeedle_matchesEverything() {
    assertThat(NormalizedText.contains("Qualquer texto", null)).isTrue();
    assertThat(NormalizedText.contains("Qualquer texto", "")).isTrue();
    assertThat(NormalizedText.contains("Qualquer texto", "   ")).isTrue();
  }

  @Test
  void nullHaystack_neverMatchesANonBlankNeedle() {
    assertThat(NormalizedText.contains(null, "reembolso")).isFalse();
  }

  @Test
  void noMatch_whenNeedleIsAbsent() {
    assertThat(NormalizedText.contains("Carteirinha digital", "boleto")).isFalse();
  }
}
