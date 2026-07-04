package com.fkmed.domain.identity;

import java.time.Duration;

/**
 * Domain-facing identity configuration, built in infra from {@code app.identity} / {@code
 * app.legal} and injected into {@link IdentityService} (so the domain never imports infra config).
 *
 * @param verificationTokenTtl how long an e-mail verification link is valid (SPEC-0002 BR5 — 24h).
 * @param termsVersion the current Terms of Use version recorded at acceptance (BR15).
 * @param privacyVersion the current Privacy Policy version recorded at acceptance (BR15).
 */
public record IdentitySettings(
    Duration verificationTokenTtl, String termsVersion, String privacyVersion) {}
