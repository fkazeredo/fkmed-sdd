package com.fkmed.domain.guides;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0012 BR9/BR10: the token's 10-minute validity window and its invalidation. */
class AttendanceTokenTest {

  private static final UUID BENEFICIARY = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-07-06T12:00:00Z");

  @Test
  void generate_expiresTenMinutesAfterNow() {
    AttendanceToken token = AttendanceToken.generate(BENEFICIARY, "123456", NOW, null);

    assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(10, ChronoUnit.MINUTES));
    assertThat(token.getCode()).isEqualTo("123456");
    assertThat(token.getInvalidatedAt()).isNull();
  }

  @Test
  void isActive_beforeExpiry_andNotInvalidated_isTrue() {
    AttendanceToken token = AttendanceToken.generate(BENEFICIARY, "123456", NOW, null);

    assertThat(token.isActive(NOW.plus(5, ChronoUnit.MINUTES))).isTrue();
  }

  @Test
  void isActive_afterExpiry_isFalse() {
    AttendanceToken token = AttendanceToken.generate(BENEFICIARY, "123456", NOW, null);

    assertThat(token.isActive(NOW.plus(10, ChronoUnit.MINUTES))).isFalse();
    assertThat(token.isActive(NOW.plus(11, ChronoUnit.MINUTES))).isFalse();
  }

  @Test
  void isActive_onceInvalidated_isFalseEvenBeforeExpiry() {
    AttendanceToken token = AttendanceToken.generate(BENEFICIARY, "123456", NOW, null);

    token.invalidate(NOW.plus(1, ChronoUnit.MINUTES));

    assertThat(token.isActive(NOW.plus(2, ChronoUnit.MINUTES))).isFalse();
  }
}
