package com.fkmed.infra.platform;

import com.fkmed.infra.identity.AppIdentityProperties;
import com.fkmed.infra.security.AppSecurityProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Fail-fast production guard (DECISIONS-BASELINE §0023): refuses to finish booting the {@code prod}
 * profile with any enumerated dev default. Runs as an {@link ApplicationRunner} so it executes
 * after Flyway has migrated — letting it detect the seeded dev account (SPEC-0002) in the database,
 * not only the config-level dev defaults. The dev defaults are listed in SECURITY.md and
 * allowlisted in {@code .gitleaks.toml}.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdReadinessValidator implements ApplicationRunner {

  static final String DEV_DB_PASSWORD = "fkmed";
  static final String DEV_ACCOUNT_EMAIL = "maria@fkmed.local";
  static final String DEV_ACCOUNT_PASSWORD = "maria12345";
  // Débito técnico B (SPEC-0003 slice 1.3): the disposable account-security E2E identity (Flyway
  // V7).
  static final String DEV_E2E_ACCOUNT_EMAIL = "seguranca-e2e@fkmed.local";
  static final String DEV_E2E_ACCOUNT_PASSWORD = "seguranca12345";
  // SPEC-0006 Phase 2: the disposable profile E2E identities (Flyway V13).
  static final String DEV_PROFILE_E2E_ACCOUNT_EMAIL = "perfil-e2e@fkmed.local";
  static final String DEV_PROFILE_E2E_ACCOUNT_PASSWORD = "perfilE2e12345";
  static final String DEV_TERMS_E2E_ACCOUNT_EMAIL = "termos-e2e@fkmed.local";
  static final String DEV_TERMS_E2E_ACCOUNT_PASSWORD = "termosE2e12345";

  private final AppSecurityProperties securityProperties;
  private final AppIdentityProperties identityProperties;
  private final Environment environment;
  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(ApplicationArguments args) {
    List<String> violations = new ArrayList<>();
    if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
      violations.add("the 'dev' profile (and its seeded login) must never be active in prod");
    }
    if (DEV_DB_PASSWORD.equals(environment.getProperty("spring.datasource.password"))) {
      violations.add("spring.datasource.password is the dev default");
    }
    if (!securityProperties.issuer().startsWith("https://")) {
      violations.add("app.security.issuer must be https in prod");
    }
    if (securityProperties.jwkPrivateKey().isBlank()) {
      violations.add(
          "app.security.jwk-private-key must be a persisted key in prod (baseline §0020)");
    }
    if (securityProperties.allowedOrigins().stream().anyMatch(o -> o.contains("localhost"))
        || securityProperties.redirectUris().stream().anyMatch(o -> o.contains("localhost"))) {
      violations.add("localhost origins/redirect URIs are dev defaults");
    }
    if (identityProperties.registrationTokenSecret().isBlank()) {
      violations.add("app.identity.registration-token-secret must be persisted in prod");
    }
    if (seedAccountPresent(DEV_ACCOUNT_EMAIL, DEV_ACCOUNT_PASSWORD)) {
      violations.add(
          "the dev seed account (" + DEV_ACCOUNT_EMAIL + ") must not exist in prod (SPEC-0002)");
    }
    if (seedAccountPresent(DEV_E2E_ACCOUNT_EMAIL, DEV_E2E_ACCOUNT_PASSWORD)) {
      violations.add(
          "the dev seed account ("
              + DEV_E2E_ACCOUNT_EMAIL
              + ") must not exist in prod (SPEC-0003 débito B)");
    }
    if (seedAccountPresent(DEV_PROFILE_E2E_ACCOUNT_EMAIL, DEV_PROFILE_E2E_ACCOUNT_PASSWORD)) {
      violations.add(
          "the dev seed account ("
              + DEV_PROFILE_E2E_ACCOUNT_EMAIL
              + ") must not exist in prod (SPEC-0006 Phase 2)");
    }
    if (seedAccountPresent(DEV_TERMS_E2E_ACCOUNT_EMAIL, DEV_TERMS_E2E_ACCOUNT_PASSWORD)) {
      violations.add(
          "the dev seed account ("
              + DEV_TERMS_E2E_ACCOUNT_EMAIL
              + ") must not exist in prod (SPEC-0006 Phase 2)");
    }
    if (!violations.isEmpty()) {
      throw new IllegalStateException(
          "production readiness check failed:\n - " + String.join("\n - ", violations));
    }
  }

  /** True when the given seeded dev account with its dev password is present in the database. */
  private boolean seedAccountPresent(String email, String devPassword) {
    List<String> hashes =
        jdbcTemplate.queryForList(
            "select password_hash from user_account where email = ?", String.class, email);
    return !hashes.isEmpty() && passwordEncoder.matches(devPassword, hashes.get(0));
  }
}
