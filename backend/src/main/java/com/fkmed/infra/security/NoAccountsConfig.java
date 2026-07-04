package com.fkmed.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Account-store seam outside the dev profile — traceable stub for SPEC-0002.
 *
 * <p>There is no real user store yet; this bean exists so Spring Boot never auto-generates its
 * default user (random password logged at startup). Every login attempt fails with a generic error
 * (no user enumeration — DECISIONS-BASELINE §0005). SPEC-0002 (Identity and Access) replaces it
 * with the DB-backed store, lockout and password policy.
 */
@Configuration
@Profile("!dev")
public class NoAccountsConfig {

  @Bean
  UserDetailsService noAccountsUserDetailsService() {
    return username -> {
      throw new UsernameNotFoundException("no local accounts (SPEC-0002 pending)");
    };
  }
}
