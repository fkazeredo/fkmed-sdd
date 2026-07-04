package com.fkmed.infra.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * DEV-ONLY LOGIN SEAM — explicit, traceable stub for SPEC-0002 (SPEC-0001 BR8).
 *
 * <p>Seeds the in-memory user {@code maria}/{@code dev12345} bound to MARIA CLARA SOUZA LIMA's
 * beneficiary record (card configured by {@code app.security.dev-login-card}) so the walking
 * skeleton has an authenticated journey before real accounts exist. Active only on the {@code dev}
 * profile; SPEC-0002 (Identity and Access) replaces it with the real account journeys and a
 * DB-backed user store. The credentials are enumerated dev defaults (SECURITY.md, {@code
 * .gitleaks.toml}) and refused in production by {@code ProdReadinessValidator}.
 */
@Configuration
@Profile("dev")
@Slf4j
public class DevLoginConfig {

  static final String DEV_USERNAME = "maria";
  static final String DEV_PASSWORD = "dev12345";

  @Bean
  UserDetailsService devUserDetailsService(PasswordEncoder passwordEncoder) {
    log.warn(
        "dev login seam active — user '{}' (SPEC-0002 stub, never in production)", DEV_USERNAME);
    return new InMemoryUserDetailsManager(
        User.withUsername(DEV_USERNAME)
            .password(passwordEncoder.encode(DEV_PASSWORD))
            .roles("BENEFICIARY")
            .build());
  }

  /** Binds the dev user to MARIA's beneficiary card in every issued access token. */
  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> devBeneficiaryClaimCustomizer(
      AppSecurityProperties properties) {
    return context -> {
      if (DEV_USERNAME.equals(context.getPrincipal().getName())
          && !properties.devLoginCard().isBlank()) {
        context.getClaims().claim(TokenClaims.BENEFICIARY_CARD, properties.devLoginCard());
      }
    };
  }
}
