package com.fkmed.domain.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0005 BR6: a banner is visible only when {@code active} and inside its optional validity
 * window (start/end both optional; when present, the boundary instants themselves are visible).
 */
class BannerTest {

  private static final Instant VALID_FROM = Instant.parse("2026-07-01T00:00:00Z");
  private static final Instant VALID_TO = Instant.parse("2026-07-10T00:00:00Z");

  private static Banner windowed(boolean active) {
    return Banner.create(
        "Título", "Texto", null, "Saiba mais", "/destino", 1, active, VALID_FROM, VALID_TO);
  }

  @Test
  void isVisibleAt_beforeTheValidityWindow_isFalse() {
    assertThat(windowed(true).isVisibleAt(VALID_FROM.minusSeconds(1))).isFalse();
  }

  @Test
  void isVisibleAt_atTheStartOfTheWindow_isTrue() {
    assertThat(windowed(true).isVisibleAt(VALID_FROM)).isTrue();
  }

  @Test
  void isVisibleAt_insideTheWindow_isTrue() {
    assertThat(windowed(true).isVisibleAt(VALID_FROM.plusSeconds(3600))).isTrue();
  }

  @Test
  void isVisibleAt_atTheEndOfTheWindow_isTrue() {
    assertThat(windowed(true).isVisibleAt(VALID_TO)).isTrue();
  }

  @Test
  void isVisibleAt_afterTheValidityWindow_isFalse() {
    assertThat(windowed(true).isVisibleAt(VALID_TO.plusSeconds(1))).isFalse();
  }

  @Test
  void isVisibleAt_withNullBounds_isAlwaysVisibleWhenActive() {
    Banner unbounded = Banner.create("T", "X", null, "B", "/d", 1, true, null, null);
    assertThat(unbounded.isVisibleAt(Instant.EPOCH)).isTrue();
    assertThat(unbounded.isVisibleAt(Instant.now())).isTrue();
  }

  @Test
  void isVisibleAt_whenInactive_isFalseEvenInsideTheWindow() {
    assertThat(windowed(false).isVisibleAt(VALID_FROM.plusSeconds(10))).isFalse();
  }
}
