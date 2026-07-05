package com.fkmed.infra.security;

import com.fkmed.domain.identity.IdentityAccounts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Adapter building the {@link UserContext} from the resource-server JWT. The beneficiary card is
 * product-sensitive and MUST NOT ride in the token (ADR-0009): it is resolved server-side from the
 * authenticated principal (the login e-mail = the JWT {@code sub}) via {@link IdentityAccounts},
 * never read from a claim. A non-JWT or unlinked principal resolves to an empty card.
 */
@Component
@RequiredArgsConstructor
public class SecurityContextUserProvider implements UserContextProvider {

  private final IdentityAccounts accounts;

  @Override
  public UserContext current() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new IllegalStateException("no authenticated user in context");
    }
    String card = null;
    if (authentication instanceof JwtAuthenticationToken) {
      card = accounts.beneficiaryCardFor(authentication.getName()).orElse(null);
    }
    return new UserContext(authentication.getName(), card);
  }
}
