package com.fkmed.application.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Authenticated password-change request (SPEC-0002 BR11). The new password is policy-validated
 * server-side and must differ from the current one ({@code PasswordPolicy.validateChange}); only
 * shape validation lives here.
 *
 * @param currentPassword the caller's current password.
 * @param newPassword the chosen new password.
 */
public record ChangePasswordRequest(
    @NotBlank String currentPassword, @NotBlank String newPassword) {}
