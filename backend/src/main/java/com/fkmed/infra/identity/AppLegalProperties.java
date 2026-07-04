package com.fkmed.infra.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Current legal-document versions recorded at registration (SPEC-0002 BR15). A config-driven seam
 * for SPEC-0006, which will own the documents themselves and the new-version re-acceptance flow.
 *
 * @param termsVersion current Terms of Use version.
 * @param privacyVersion current Privacy Policy version.
 */
@ConfigurationProperties(prefix = "app.legal")
public record AppLegalProperties(
    @DefaultValue("1.0") String termsVersion, @DefaultValue("1.0") String privacyVersion) {}
