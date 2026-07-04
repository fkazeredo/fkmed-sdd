package com.fkmed.application.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request for a fresh verification link (SPEC-0002 BR5). The response is always neutral (DL-0001).
 *
 * @param email the account's login e-mail.
 */
public record ResendVerificationRequest(@NotBlank @Email String email) {}
