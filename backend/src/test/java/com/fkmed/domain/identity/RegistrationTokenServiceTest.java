package com.fkmed.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0002 / DL-0001: the stateless HMAC registration token round-trip, tamper and expiry. */
class RegistrationTokenServiceTest {

  private static final byte[] SECRET = "registration-secret".getBytes(StandardCharsets.UTF_8);
  private static final Instant NOW = Instant.parse("2026-07-04T12:00:00Z");
  private static final Duration TTL = Duration.ofMinutes(30);
  private static final UUID BENEFICIARY = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");

  private static RegistrationTokenService at(Instant instant) {
    return new RegistrationTokenService(SECRET, Clock.fixed(instant, ZoneOffset.UTC), TTL);
  }

  @Test
  void issue_thenVerify_returnsTheSameBeneficiary() {
    RegistrationTokenService service = at(NOW);
    assertThat(service.verify(service.issue(BENEFICIARY))).isEqualTo(BENEFICIARY);
  }

  @Test
  void verify_rejectsAMalformedToken() {
    assertThatExceptionOfType(RegistrationNotFoundException.class)
        .isThrownBy(() -> at(NOW).verify("not-a-token"));
  }

  @Test
  void verify_rejectsATamperedSignature() {
    String token = at(NOW).issue(BENEFICIARY);
    String tampered = token.substring(0, token.indexOf('.') + 1) + "AAAA";
    assertThatExceptionOfType(RegistrationNotFoundException.class)
        .isThrownBy(() -> at(NOW).verify(tampered));
  }

  @Test
  void verify_rejectsAForeignSecret() {
    String token = at(NOW).issue(BENEFICIARY);
    RegistrationTokenService other =
        new RegistrationTokenService(
            "another-secret".getBytes(StandardCharsets.UTF_8),
            Clock.fixed(NOW, ZoneOffset.UTC),
            TTL);
    assertThatExceptionOfType(RegistrationNotFoundException.class)
        .isThrownBy(() -> other.verify(token));
  }

  @Test
  void verify_acceptsJustBeforeExpiry() {
    String token = at(NOW).issue(BENEFICIARY);
    RegistrationTokenService almostExpired = at(NOW.plus(TTL).minusSeconds(1));
    assertThat(almostExpired.verify(token)).isEqualTo(BENEFICIARY);
  }

  @Test
  void verify_rejectsAtExpiryBoundary() {
    String token = at(NOW).issue(BENEFICIARY);
    RegistrationTokenService expired = at(NOW.plus(TTL));
    assertThatExceptionOfType(RegistrationNotFoundException.class)
        .isThrownBy(() -> expired.verify(token));
  }
}
