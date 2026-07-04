package com.fkmed.infra.identity;

import com.fkmed.domain.identity.IdentitySettings;
import com.fkmed.domain.identity.RegistrationTokenService;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the identity domain's config-carrying collaborators from {@code app.identity} / {@code
 * app.legal}, so the domain never imports infra configuration (baseline §0012).
 */
@Configuration
@Slf4j
public class IdentityInfraConfig {

  @Bean
  RegistrationTokenService registrationTokenService(AppIdentityProperties properties, Clock clock) {
    byte[] secret =
        properties.registrationTokenSecret().isBlank()
            ? ephemeralSecret()
            : properties.registrationTokenSecret().getBytes(StandardCharsets.UTF_8);
    return new RegistrationTokenService(
        secret, clock, Duration.ofMinutes(properties.registrationTokenTtlMinutes()));
  }

  @Bean
  IdentitySettings identitySettings(AppIdentityProperties identity, AppLegalProperties legal) {
    return new IdentitySettings(
        Duration.ofHours(identity.verificationTtlHours()),
        legal.termsVersion(),
        legal.privacyVersion());
  }

  private static byte[] ephemeralSecret() {
    log.warn(
        "no app.identity.registration-token-secret configured — generating an EPHEMERAL secret "
            + "(dev only; production requires a persisted value, refused by ProdReadinessValidator)");
    byte[] secret = new byte[32];
    new SecureRandom().nextBytes(secret);
    return secret;
  }
}
