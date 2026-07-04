package com.fkmed.application.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * E-mail verification confirmation (SPEC-0002 BR5): the raw token from the verification link.
 *
 * @param token the raw verification token.
 */
public record ConfirmVerificationRequest(@NotBlank String token) {}
