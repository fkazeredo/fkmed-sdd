package com.fkmed.infra.platform;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.fkmed.infra.security.AppSecurityProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** DECISIONS-BASELINE §0023: the prod profile refuses every enumerated dev default. */
class ProdReadinessValidatorTest {

  private static AppSecurityProperties devDefaults() {
    return new AppSecurityProperties(
        "http://localhost:8080",
        List.of("http://localhost:4200"),
        List.of("http://localhost:4200"),
        List.of("http://localhost:4200"),
        "",
        "001234567");
  }

  private static AppSecurityProperties productionValues() {
    return new AppSecurityProperties(
        "https://fkmed.example.com",
        List.of(),
        List.of("https://fkmed.example.com"),
        List.of("https://fkmed.example.com"),
        "-----BEGIN PRIVATE KEY-----persisted-----END PRIVATE KEY-----",
        "");
  }

  @Test
  void refusesToBoot_withDevDefaults_listingEveryViolation() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod", "dev");
    environment.setProperty("spring.datasource.password", "fkmed");

    ProdReadinessValidator validator = new ProdReadinessValidator(devDefaults(), environment);

    assertThatIllegalStateException()
        .isThrownBy(validator::afterPropertiesSet)
        .withMessageContaining("dev' profile")
        .withMessageContaining("datasource.password")
        .withMessageContaining("issuer")
        .withMessageContaining("jwk-private-key")
        .withMessageContaining("localhost")
        .withMessageContaining("dev-login-card");
  }

  @Test
  void boots_withProductionValues() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    environment.setProperty("spring.datasource.password", "a-real-secret-from-env");

    ProdReadinessValidator validator = new ProdReadinessValidator(productionValues(), environment);

    assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
  }
}
