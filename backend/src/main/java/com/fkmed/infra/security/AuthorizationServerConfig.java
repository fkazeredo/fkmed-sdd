package com.fkmed.infra.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

/**
 * Embedded Spring Authorization Server wiring (DECISIONS-BASELINE §0018/§0020): JDBC-persisted
 * client and authorization state, env-injectable signing key, and the {@code fkmed-web} public SPA
 * client (Authorization Code + PKCE, no refresh token in the browser).
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuthorizationServerConfig {

  /** OAuth2/OIDC client id of the Angular SPA. */
  public static final String WEB_CLIENT_ID = "fkmed-web";

  @Bean
  RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcRegisteredClientRepository(jdbcTemplate);
  }

  @Bean
  OAuth2AuthorizationService authorizationService(
      JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
    return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
  }

  @Bean
  OAuth2AuthorizationConsentService authorizationConsentService(
      JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
    return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
  }

  @Bean
  AuthorizationServerSettings authorizationServerSettings(AppSecurityProperties properties) {
    return AuthorizationServerSettings.builder().issuer(properties.issuer()).build();
  }

  /**
   * Signing key for issued tokens. A persisted PEM key comes from the environment (production —
   * baseline §0020); when absent, an ephemeral dev-only key is generated and a warning logged.
   */
  @Bean
  JWKSource<SecurityContext> jwkSource(AppSecurityProperties properties) {
    RSAKey rsaKey =
        properties.jwkPrivateKey().isBlank()
            ? generateEphemeralKey()
            : parsePemKey(properties.jwkPrivateKey());
    return new ImmutableJWKSet<>(new JWKSet(rsaKey));
  }

  /** The resource server validates the AS's own tokens locally — no self-HTTP discovery. */
  @Bean
  JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  /** Seeds/updates the SPA public client from configuration at startup (idempotent upsert). */
  @Bean
  ApplicationRunner registeredClientSeeder(
      RegisteredClientRepository repository, AppSecurityProperties properties) {
    return args -> {
      RegisteredClient existing = repository.findByClientId(WEB_CLIENT_ID);
      String id = existing != null ? existing.getId() : UUID.randomUUID().toString();
      RegisteredClient.Builder builder =
          RegisteredClient.withId(id)
              .clientId(WEB_CLIENT_ID)
              .clientName("FKMed Web")
              .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .scope("openid")
              .scope("profile")
              .clientSettings(
                  ClientSettings.builder()
                      .requireProofKey(true)
                      .requireAuthorizationConsent(false)
                      .build())
              .tokenSettings(
                  TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(15)).build());
      properties.redirectUris().forEach(builder::redirectUri);
      properties.postLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
      repository.save(builder.build());
      log.info(
          "registered client '{}' seeded with redirect URIs {}",
          WEB_CLIENT_ID,
          properties.redirectUris());
    };
  }

  private RSAKey generateEphemeralKey() {
    log.warn(
        "no app.security.jwk-private-key configured — generating an EPHEMERAL signing key "
            + "(dev only; production requires a persisted key, DECISIONS-BASELINE §0020)");
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair pair = generator.generateKeyPair();
      return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
          .privateKey(pair.getPrivate())
          .keyID(UUID.randomUUID().toString())
          .build();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("RSA unavailable", e);
    }
  }

  private RSAKey parsePemKey(String pem) {
    try {
      String base64 =
          pem.replace("-----BEGIN PRIVATE KEY-----", "")
              .replace("-----END PRIVATE KEY-----", "")
              .replaceAll("\\s", "");
      byte[] der = Base64.getDecoder().decode(base64);
      KeyFactory factory = KeyFactory.getInstance("RSA");
      RSAPrivateCrtKey privateKey =
          (RSAPrivateCrtKey) factory.generatePrivate(new PKCS8EncodedKeySpec(der));
      RSAPublicKey publicKey =
          (RSAPublicKey)
              factory.generatePublic(
                  new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
      RSAKey key = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
      return new RSAKey.Builder(publicKey)
          .privateKey(privateKey)
          .keyID(key.computeThumbprint().toString())
          .build();
    } catch (Exception e) {
      throw new IllegalStateException("invalid app.security.jwk-private-key (PKCS#8 PEM)", e);
    }
  }
}
