package com.fkmed.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** The UserContextProvider adapter reads identity and beneficiary card from the JWT. */
class SecurityContextUserProviderTest {

  private final SecurityContextUserProvider provider = new SecurityContextUserProvider();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void readsUsernameAndBeneficiaryCard_fromTheJwt() {
    Jwt jwt =
        new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "RS256"),
            Map.of("sub", "maria", TokenClaims.BENEFICIARY_CARD, "001234567"));
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

    UserContext context = provider.current();

    assertThat(context.username()).isEqualTo("maria");
    assertThat(context.beneficiaryCard()).contains("001234567");
  }

  @Test
  void beneficiaryCard_isEmpty_whenTheClaimIsAbsent() {
    Jwt jwt =
        new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "RS256"),
            Map.of("sub", "someone"));
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

    assertThat(provider.current().beneficiaryCard()).isEmpty();
  }

  @Test
  void nonJwtAuthentication_hasNoBeneficiaryCard() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("dev", "n/a"));
    assertThat(provider.current().beneficiaryCard()).isEmpty();
  }

  @Test
  void missingAuthentication_failsFast() {
    assertThatIllegalStateException().isThrownBy(provider::current);
  }
}
