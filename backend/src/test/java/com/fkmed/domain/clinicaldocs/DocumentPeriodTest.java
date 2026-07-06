package com.fkmed.domain.clinicaldocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** SPEC-0011 BR2: the resolved list-filter date range (last N days or a custom range). */
class DocumentPeriodTest {

  @Test
  void includes_boundaryDatesInclusive() {
    DocumentPeriod period = new DocumentPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

    assertThat(period.includes(LocalDate.of(2026, 1, 1))).isTrue();
    assertThat(period.includes(LocalDate.of(2026, 1, 31))).isTrue();
    assertThat(period.includes(LocalDate.of(2025, 12, 31))).isFalse();
    assertThat(period.includes(LocalDate.of(2026, 2, 1))).isFalse();
  }

  @Test
  void constructor_rejectsFromAfterTo() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new DocumentPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
  }

  @Test
  void constructor_rejectsNullBounds() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new DocumentPeriod(null, LocalDate.of(2026, 1, 1)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new DocumentPeriod(LocalDate.of(2026, 1, 1), null));
  }
}
