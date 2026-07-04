package com.fkmed.infra.platform;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fkmed.infra.identity.AppIdentityProperties;
import com.fkmed.infra.security.AppSecurityProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/** DECISIONS-BASELINE §0023: the prod profile refuses every enumerated dev default. */
class ProdReadinessValidatorTest {

  private static final PasswordEncoder ENCODER =
      PasswordEncoderFactories.createDelegatingPasswordEncoder();

  private static AppSecurityProperties devSecurity() {
    return new AppSecurityProperties(
        "http://localhost:8080",
        List.of("http://localhost:4200"),
        List.of("http://localhost:4200"),
        List.of("http://localhost:4200"),
        "");
  }

  private static AppSecurityProperties productionSecurity() {
    return new AppSecurityProperties(
        "https://fkmed.example.com",
        List.of(),
        List.of("https://fkmed.example.com"),
        List.of("https://fkmed.example.com"),
        "-----BEGIN PRIVATE KEY-----persisted-----END PRIVATE KEY-----");
  }

  private static AppIdentityProperties devIdentity() {
    return new AppIdentityProperties("", "http://localhost:4200", 24, 30);
  }

  private static AppIdentityProperties productionIdentity() {
    return new AppIdentityProperties(
        "a-real-registration-secret", "https://fkmed.example.com", 24, 30);
  }

  @SuppressWarnings("unchecked")
  private static JdbcTemplate noDevAccount() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForList(anyString(), eq(String.class), any())).thenReturn(List.of());
    return jdbc;
  }

  @Test
  void refusesToBoot_withDevDefaults_listingEveryViolation() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod", "dev");
    environment.setProperty("spring.datasource.password", "fkmed");

    ProdReadinessValidator validator =
        new ProdReadinessValidator(
            devSecurity(), devIdentity(), environment, noDevAccount(), ENCODER);

    assertThatIllegalStateException()
        .isThrownBy(() -> validator.run(null))
        .withMessageContaining("dev' profile")
        .withMessageContaining("datasource.password")
        .withMessageContaining("issuer")
        .withMessageContaining("jwk-private-key")
        .withMessageContaining("localhost")
        .withMessageContaining("registration-token-secret");
  }

  @Test
  void refusesToBoot_whenTheDevSeedAccountIsPresent() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    environment.setProperty("spring.datasource.password", "a-real-secret-from-env");

    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForList(anyString(), eq(String.class), any()))
        .thenReturn(List.of(ENCODER.encode("maria12345")));

    ProdReadinessValidator validator =
        new ProdReadinessValidator(
            productionSecurity(), productionIdentity(), environment, jdbc, ENCODER);

    assertThatIllegalStateException()
        .isThrownBy(() -> validator.run(null))
        .withMessageContaining("dev seed account");
  }

  @Test
  void boots_withProductionValues() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    environment.setProperty("spring.datasource.password", "a-real-secret-from-env");

    ProdReadinessValidator validator =
        new ProdReadinessValidator(
            productionSecurity(), productionIdentity(), environment, noDevAccount(), ENCODER);

    assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
  }
}
