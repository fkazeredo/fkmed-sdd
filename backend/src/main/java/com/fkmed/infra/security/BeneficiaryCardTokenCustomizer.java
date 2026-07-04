package com.fkmed.infra.security;

import com.fkmed.domain.identity.IdentityAccounts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * Injects the {@code beneficiary_card} claim into issued tokens (SPEC-0002; reuses {@link
 * TokenClaims}) by resolving the authenticated account's linked beneficiary. Replaces the dev-seam
 * customizer of SPEC-0001 BR8.
 */
@Component
@RequiredArgsConstructor
public class BeneficiaryCardTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

  private final IdentityAccounts accounts;

  @Override
  public void customize(JwtEncodingContext context) {
    accounts
        .beneficiaryCardFor(context.getPrincipal().getName())
        .ifPresent(card -> context.getClaims().claim(TokenClaims.BENEFICIARY_CARD, card));
  }
}
