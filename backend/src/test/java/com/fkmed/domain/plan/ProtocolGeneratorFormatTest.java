package com.fkmed.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0003 BR9 / DL-0016: the protocol string shape (the DB-backed counter is covered by an IT).
 */
class ProtocolGeneratorFormatTest {

  @Test
  void formats_asPrefixDateFourDigitCounter() {
    assertThat(ProtocolGenerator.format("AG", LocalDate.of(2026, 7, 6), 1))
        .isEqualTo("AG-20260706-0001");
    assertThat(ProtocolGenerator.format("AG", LocalDate.of(2026, 12, 31), 42))
        .isEqualTo("AG-20261231-0042");
  }

  @Test
  void matches_theCanonicalRegex() {
    String protocol = ProtocolGenerator.format("RE", LocalDate.of(2026, 1, 9), 1234);
    assertThat(protocol).matches("^[A-Z]{2}-\\d{8}-\\d{4}$");
  }
}
