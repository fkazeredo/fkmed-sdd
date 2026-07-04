package com.fkmed.application.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Account-creation request (SPEC-0002 BR4/BR9/BR15). Terms and Privacy acceptance are recorded
 * against the current server-side versions (DL-0001); the password policy is enforced in the domain
 * ({@code PasswordPolicy}), so only shape validation lives here.
 *
 * @param registrationToken the token returned by {@code first-access/verify}.
 * @param email the login e-mail (unique, valid, ≤ 160 chars).
 * @param password the chosen password (policy validated server-side).
 * @param acceptedTerms MUST be true — acceptance of the current Terms of Use.
 * @param acceptedPrivacy MUST be true — acceptance of the current Privacy Policy.
 */
public record CompleteFirstAccessRequest(
    @NotBlank String registrationToken,
    @NotBlank @Email @Size(max = 160) String email,
    @NotBlank String password,
    @AssertTrue(message = "the Terms of Use must be accepted") boolean acceptedTerms,
    @AssertTrue(message = "the Privacy Policy must be accepted") boolean acceptedPrivacy) {}
