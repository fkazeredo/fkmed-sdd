package com.fkmed.domain.telemedicine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The pure queue arithmetic (SPEC-0010 BR5/BR6, §I/O Examples: position 4 → 12 minutes). */
class TeleQueueTest {

  @Test
  void positionFrom_isOneBasedOnTheSessionsAhead() {
    assertThat(TeleQueue.positionFrom(0)).isEqualTo(1);
    assertThat(TeleQueue.positionFrom(3)).isEqualTo(4);
  }

  @Test
  void etaMinutes_isThreeMinutesPerPlace() {
    assertThat(TeleQueue.etaMinutes(1)).isEqualTo(3);
    assertThat(TeleQueue.etaMinutes(4)).isEqualTo(12);
  }
}
