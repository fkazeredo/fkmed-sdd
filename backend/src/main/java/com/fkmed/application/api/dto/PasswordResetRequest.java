package com.fkmed.application.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Password-reset request from an e-mailed link (SPEC-0002 BR10). The new password is
 * policy-validated server-side ({@code PasswordPolicy}); only shape validation lives here.
 *
 * @param token the raw one-time reset token from the e-mail.
 * @param newPassword the chosen new password.
 */
public record PasswordResetRequest(@NotBlank String token, @NotBlank String newPassword) {}
