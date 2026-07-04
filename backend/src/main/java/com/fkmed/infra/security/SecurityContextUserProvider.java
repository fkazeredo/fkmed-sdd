package com.fkmed.infra.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Adapter reading the {@link UserContext} from the resource-server JWT. */
@Component
public class SecurityContextUserProvider implements UserContextProvider {

  @Override
  public UserContext current() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new IllegalStateException("no authenticated user in context");
    }
    String card = null;
    if (authentication instanceof JwtAuthenticationToken jwt) {
      card = jwt.getToken().getClaimAsString(TokenClaims.BENEFICIARY_CARD);
    }
    return new UserContext(authentication.getName(), card);
  }
}
