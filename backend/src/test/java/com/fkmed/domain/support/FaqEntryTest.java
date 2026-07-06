package com.fkmed.domain.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * SPEC-0014 BR5: the FAQ search filters over BOTH the question (title) and the answer (content),
 * case/accent-insensitive.
 */
class FaqEntryTest {

  private static final FaqEntry ENTRY =
      FaqEntry.create(
          FaqCategoryCodes.REEMBOLSO,
          "Até quando posso solicitar o reembolso de uma despesa?",
          "Você tem até 12 meses a partir da data do atendimento para solicitar o reembolso.",
          1,
          true);

  @Test
  void matchesQuery_whenTermIsInTheQuestion() {
    assertThat(ENTRY.matchesQuery("reembolso")).isTrue();
    assertThat(ENTRY.matchesQuery("REEMBOLSO")).isTrue();
  }

  @Test
  void matchesQuery_whenTermIsOnlyInTheAnswer() {
    assertThat(ENTRY.matchesQuery("12 meses")).isTrue();
  }

  @Test
  void matchesQuery_isAccentInsensitive() {
    assertThat(ENTRY.matchesQuery("ate quando")).isTrue();
  }

  @Test
  void matchesQuery_blankOrNullTerm_matchesEverything() {
    assertThat(ENTRY.matchesQuery(null)).isTrue();
    assertThat(ENTRY.matchesQuery("")).isTrue();
    assertThat(ENTRY.matchesQuery("   ")).isTrue();
  }

  @Test
  void matchesQuery_returnsFalse_whenTermIsAbsentFromBothFields() {
    assertThat(ENTRY.matchesQuery("carteirinha")).isFalse();
  }
}
