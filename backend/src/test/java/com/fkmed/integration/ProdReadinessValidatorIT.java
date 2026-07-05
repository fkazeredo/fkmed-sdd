package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.fkmed.infra.identity.AppIdentityProperties;
import com.fkmed.infra.platform.ProdReadinessValidator;
import com.fkmed.infra.security.AppSecurityProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DECISIONS-BASELINE §0023 / SPEC-0002: {@link ProdReadinessValidator}'s dev-seed-account guard
 * against the REAL seeded MARIA row (Flyway V3) in a real Postgres — not a mocked {@code
 * JdbcTemplate} (which is all {@code ProdReadinessValidatorTest} exercises). This is the one test
 * that proves the actual SQL query shape and the actual delegating {@link PasswordEncoder}
 * correctly match the real bcrypt hash written by the migration, so a future refactor (query shape,
 * encoder swap, column rename) cannot break this production safety net silently.
 */
class ProdReadinessValidatorIT extends AbstractIntegrationTest {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private PasswordEncoder passwordEncoder;

  private static AppSecurityProperties productionSecurity() {
    return new AppSecurityProperties(
        "https://fkmed.example.com",
        List.of(),
        List.of("https://fkmed.example.com"),
        List.of("https://fkmed.example.com"),
        "-----BEGIN PRIVATE KEY-----persisted-----END PRIVATE KEY-----");
  }

  private static AppIdentityProperties productionIdentity() {
    return new AppIdentityProperties(
        "a-real-registration-secret", "https://fkmed.example.com", 24, 30, 30);
  }

  @Test
  void refusesToBoot_becauseTheRealSeededMariaAccountIsPresent() {
    // The MARIA seed (V3) is the row stably present across the shared-Postgres suite (sibling ITs
    // delete every OTHER user_account for isolation, but preserve MARIA), so it is the safe fixture
    // for exercising the guard against the REAL bcrypt hash + SQL + encoder. The disposable-account
    // guard (V7, débito B) reuses the exact same seedAccountPresent(...) path and is proven by
    // ProdReadinessValidatorTest (its own real row is ephemeral here because those cleaners drop
    // it).
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    environment.setProperty("spring.datasource.password", "a-real-secret-from-env");

    ProdReadinessValidator validator =
        new ProdReadinessValidator(
            productionSecurity(), productionIdentity(), environment, jdbc, passwordEncoder);

    assertThatIllegalStateException()
        .isThrownBy(() -> validator.run(null))
        .withMessageContaining("dev seed account")
        .withMessageContaining("maria@fkmed.local");
  }
}
