package com.fkmed.application.api.dto;

/**
 * Result of a successful identity verification (SPEC-0002 BR1): the registration token to present
 * at {@code first-access/complete}.
 *
 * @param registrationToken short-lived stateless token bridging verify → complete (DL-0001).
 */
public record VerifyFirstAccessResponse(String registrationToken) {}
