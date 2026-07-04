package com.fkmed.infra.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Security configuration of the embedded Authorization Server (DECISIONS-BASELINE §0018).
 *
 * @param issuer the OIDC issuer URL (token {@code iss} claim and discovery document base).
 * @param allowedOrigins CORS origins allowed to call the token/discovery endpoints (dev SPA only;
 *     production is same-origin behind the TLS proxy).
 * @param redirectUris redirect URIs registered for the {@code fkmed-web} public client.
 * @param postLogoutRedirectUris post-logout redirect URIs registered for {@code fkmed-web}.
 * @param jwkPrivateKey PEM (PKCS#8) RSA private key used to sign tokens. Blank generates an
 *     ephemeral dev-only key; production requires a persisted key (baseline §0020) — enforced by
 *     {@code ProdReadinessValidator}.
 * @param devLoginCard beneficiary card bound to the dev-profile login seam (SPEC-0001 BR8; replaced
 *     by SPEC-0002).
 */
@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
    @DefaultValue("http://localhost:8080") String issuer,
    @DefaultValue List<String> allowedOrigins,
    @DefaultValue("http://localhost:4200") List<String> redirectUris,
    @DefaultValue("http://localhost:4200") List<String> postLogoutRedirectUris,
    @DefaultValue("") String jwkPrivateKey,
    @DefaultValue("") String devLoginCard) {}
