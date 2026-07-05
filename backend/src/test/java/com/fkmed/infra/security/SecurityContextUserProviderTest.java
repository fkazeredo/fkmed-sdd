package com.fkmed.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fkmed.domain.identity.IdentityAccounts;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The UserContextProvider adapter takes the username from the JWT subject and resolves the
 * beneficiary card server-side from that principal (ADR-0009), never from a token claim.
 */
class SecurityContextUserProviderTest {

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_CARD = "001234567";

  private final IdentityAccounts accounts = mock(IdentityAccounts.class);
  private final SecurityContextUserProvider provider = new SecurityContextUserProvider(accounts);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void readsUsernameFromSubject_andResolvesBeneficiaryCardServerSide() {
    when(accounts.beneficiaryCardFor(MARIA_EMAIL)).thenReturn(Optional.of(MARIA_CARD));
    Jwt jwt =
        new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "RS256"),
            Map.of("sub", MARIA_EMAIL));
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

    UserContext context = provider.current();

    assertThat(context.username()).isEqualTo(MARIA_EMAIL);
    assertThat(context.beneficiaryCard()).contains(MARIA_CARD);
  }

  @Test
  void beneficiaryCard_isEmpty_whenThePrincipalResolvesToNoCard() {
    when(accounts.beneficiaryCardFor("someone")).thenReturn(Optional.empty());
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
  void nonJwtAuthentication_hasNoBeneficiaryCard_andIsNotResolved() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("dev", "n/a"));

    assertThat(provider.current().beneficiaryCard()).isEmpty();
    verifyNoInteractions(accounts);
  }

  @Test
  void missingAuthentication_failsFast() {
    assertThatIllegalStateException().isThrownBy(provider::current);
  }
}
