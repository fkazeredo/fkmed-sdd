package com.fkmed.domain.finance;

import java.time.LocalDate;

/**
 * Formats a competência (the invoice's reference month) as the pt-BR "Mês/AAAA" label the API and
 * PDFs display (SPEC-0013 BR2), e.g. {@code 2026-07-01 → "Julho/2026"}. Month names are a fixed
 * pt-BR table (no {@code Locale} dependency, so the label is deterministic regardless of the JVM's
 * default locale).
 */
final class Competencia {

  private static final String[] MONTHS = {
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
  };

  private Competencia() {}

  static String label(LocalDate competencia) {
    return MONTHS[competencia.getMonthValue() - 1] + "/" + competencia.getYear();
  }
}
