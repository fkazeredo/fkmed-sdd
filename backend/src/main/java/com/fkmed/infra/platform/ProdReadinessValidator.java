package com.fkmed.infra.platform;

import com.fkmed.infra.security.AppSecurityProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail-fast production guard (DECISIONS-BASELINE §0023): refuses to boot the {@code prod} profile
 * with any enumerated dev default. The dev defaults themselves are listed in SECURITY.md and
 * allowlisted in {@code .gitleaks.toml}.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdReadinessValidator implements InitializingBean {

  static final String DEV_DB_PASSWORD = "fkmed";

  private final AppSecurityProperties securityProperties;
  private final Environment environment;

  @Override
  public void afterPropertiesSet() {
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
    if (!securityProperties.devLoginCard().isBlank()) {
      violations.add("app.security.dev-login-card (SPEC-0002 dev seam) must be empty in prod");
    }
    if (!violations.isEmpty()) {
      throw new IllegalStateException(
          "production readiness check failed:\n - " + String.join("\n - ", violations));
    }
  }
}
