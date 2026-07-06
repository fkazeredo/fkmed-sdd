package com.fkmed.domain.plan;

/**
 * A beneficiary's stored photo for delivery (SPEC-0006 BR3): the raw image bytes and their content
 * type, so the endpoint can serve the avatar with the right {@code Content-Type}.
 */
public record PhotoContent(byte[] image, String contentType) {}
