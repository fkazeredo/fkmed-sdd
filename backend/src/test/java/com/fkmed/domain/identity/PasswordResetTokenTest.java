package com.fkmed.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0002 BR10: reset-link usability, the 30-minute expiry boundary and single-use. */
class PasswordResetTokenTest {

  private static final Instant ISSUED = Instant.parse("2026-07-04T12:00:00Z");
  private static final Duration TTL = Duration.ofMinutes(30);

  private static PasswordResetToken issued() {
    return PasswordResetToken.issue(UUID.randomUUID(), "hash", ISSUED, TTL);
  }

  @Test
  void isUsable_whileUnusedAndUnexpired() {
    assertThat(issued().isUsable(ISSUED.plus(Duration.ofMinutes(29)))).isTrue();
  }

  @Test
  void expiresExactlyAtTheTtlBoundary() {
    PasswordResetToken token = issued();
    assertThat(token.getExpiresAt()).isEqualTo(ISSUED.plus(TTL));
    assertThat(token.isUsable(ISSUED.plus(TTL))).isFalse();
  }

  @Test
  void isNotUsable_afterExpiry() {
    assertThat(issued().isUsable(ISSUED.plus(Duration.ofMinutes(31)))).isFalse();
  }

  @Test
  void markUsed_makesItSingleUse() {
    PasswordResetToken token = issued();
    token.markUsed(ISSUED.plus(Duration.ofMinutes(1)));
    assertThat(token.isUsable(ISSUED.plus(Duration.ofMinutes(2)))).isFalse();
    assertThat(token.getUsedAt()).isEqualTo(ISSUED.plus(Duration.ofMinutes(1)));
  }

  @Test
  void markUsed_isIdempotent_keepingTheFirstConsumptionTimestamp() {
    PasswordResetToken token = issued();
    Instant first = ISSUED.plus(Duration.ofMinutes(1));
    token.markUsed(first);
    token.markUsed(ISSUED.plus(Duration.ofMinutes(5)));
    assertThat(token.getUsedAt()).isEqualTo(first);
  }
}
