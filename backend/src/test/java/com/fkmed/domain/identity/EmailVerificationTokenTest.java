package com.fkmed.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0002 BR5: verification-link usability, expiry and single-use. */
class EmailVerificationTokenTest {

  private static final Instant ISSUED = Instant.parse("2026-07-04T12:00:00Z");
  private static final Duration TTL = Duration.ofHours(24);

  private static EmailVerificationToken issued() {
    return EmailVerificationToken.issue(UUID.randomUUID(), "hash", ISSUED, TTL);
  }

  @Test
  void isUsable_whileUnusedAndUnexpired() {
    assertThat(issued().isUsable(ISSUED.plus(Duration.ofHours(23)))).isTrue();
  }

  @Test
  void expiresExactlyAtTheTtlBoundary() {
    EmailVerificationToken token = issued();
    assertThat(token.getExpiresAt()).isEqualTo(ISSUED.plus(TTL));
    assertThat(token.isUsable(ISSUED.plus(TTL))).isFalse();
  }

  @Test
  void isNotUsable_afterExpiry() {
    assertThat(issued().isUsable(ISSUED.plus(Duration.ofHours(25)))).isFalse();
  }

  @Test
  void markUsed_makesItSingleUse() {
    EmailVerificationToken token = issued();
    token.markUsed(ISSUED.plus(Duration.ofMinutes(1)));
    assertThat(token.isUsable(ISSUED.plus(Duration.ofHours(1)))).isFalse();
    assertThat(token.getUsedAt()).isEqualTo(ISSUED.plus(Duration.ofMinutes(1)));
  }
}
