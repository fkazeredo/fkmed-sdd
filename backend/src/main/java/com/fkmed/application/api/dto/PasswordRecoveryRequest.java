package com.fkmed.application.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Password-recovery request (SPEC-0002 BR10). The response is always neutral (BR7, DL-0003).
 *
 * @param email the account's login e-mail.
 */
public record PasswordRecoveryRequest(@NotBlank @Email String email) {}
