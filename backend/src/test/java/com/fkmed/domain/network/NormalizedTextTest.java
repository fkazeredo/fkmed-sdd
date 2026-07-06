package com.fkmed.domain.network;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SPEC-0008 BR2/BR8: case/accent-insensitive substring matching for selectors and name search. */
class NormalizedTextTest {

  @Test
  void matchesRegardlessOfCase() {
    assertThat(NormalizedText.contains("Rio de Janeiro", "rio")).isTrue();
    assertThat(NormalizedText.contains("Rio de Janeiro", "RIO")).isTrue();
    assertThat(NormalizedText.contains("Cardiologia", "CARDIO")).isTrue();
  }

  @Test
  void matchesRegardlessOfAccents() {
    assertThat(NormalizedText.contains("Cardiológica", "cardiologica")).isTrue();
    assertThat(NormalizedText.contains("Niterói", "niteroi")).isTrue();
    assertThat(NormalizedText.contains("São José do Vale do Rio Preto", "sao jose")).isTrue();
  }

  @Test
  void matchesAsSubstring_anyPosition() {
    assertThat(NormalizedText.contains("Cabo Frio", "rio")).isTrue();
    assertThat(NormalizedText.contains("Três Rios", "rio")).isTrue();
  }

  @Test
  void blankOrNullNeedle_matchesEverything() {
    assertThat(NormalizedText.contains("Niterói", null)).isTrue();
    assertThat(NormalizedText.contains("Niterói", "")).isTrue();
    assertThat(NormalizedText.contains("Niterói", "   ")).isTrue();
  }

  @Test
  void nullHaystack_neverMatchesANonBlankNeedle() {
    assertThat(NormalizedText.contains(null, "rio")).isFalse();
  }

  @Test
  void noMatch_whenNeedleIsAbsent() {
    assertThat(NormalizedText.contains("Niterói", "curitiba")).isFalse();
  }
}
