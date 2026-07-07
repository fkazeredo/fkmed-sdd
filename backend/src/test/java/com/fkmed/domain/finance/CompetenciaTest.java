package com.fkmed.domain.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** SPEC-0013 BR2: the "Mês/AAAA" competência label in pt-BR. */
class CompetenciaTest {

  @Test
  void formatsTheMonthNameInPtBrAndTheYear() {
    assertThat(Competencia.label(LocalDate.of(2026, 7, 1))).isEqualTo("Julho/2026");
    assertThat(Competencia.label(LocalDate.of(2025, 3, 15))).isEqualTo("Março/2025");
    assertThat(Competencia.label(LocalDate.of(2025, 12, 1))).isEqualTo("Dezembro/2025");
  }
}
