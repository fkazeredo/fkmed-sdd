package com.fkmed.infra.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Identity configuration (SPEC-0002).
 *
 * @param registrationTokenSecret HMAC secret for the stateless registration token (DL-0001). Blank
 *     generates an ephemeral dev-only secret; production requires a persisted value (enforced by
 *     {@code ProdReadinessValidator}).
 * @param verificationBaseUrl base URL the verification link points at (the SPA origin serving
 *     {@code /verificar-email}).
 * @param verificationTtlHours lifetime of the e-mail verification link in hours (BR5 — 24).
 * @param registrationTokenTtlMinutes lifetime of the registration token in minutes (DL-0001 — 30).
 */
@ConfigurationProperties(prefix = "app.identity")
public record AppIdentityProperties(
    @DefaultValue("") String registrationTokenSecret,
    @DefaultValue("http://localhost:4200") String verificationBaseUrl,
    @DefaultValue("24") int verificationTtlHours,
    @DefaultValue("30") int registrationTokenTtlMinutes) {}
