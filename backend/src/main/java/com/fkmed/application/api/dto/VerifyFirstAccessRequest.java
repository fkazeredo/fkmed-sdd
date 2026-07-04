package com.fkmed.application.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * First-access identity-triple request (SPEC-0002 BR1). Server-side format validation mirrors the
 * client (BR16); the actual match against the beneficiary base is done in the domain.
 *
 * @param cpf 11 numeric digits.
 * @param cardNumber 9 numeric digits.
 * @param birthDate a past calendar date (ISO-8601).
 */
public record VerifyFirstAccessRequest(
    @Pattern(regexp = "\\d{11}", message = "cpf must have 11 digits") String cpf,
    @Pattern(regexp = "\\d{9}", message = "cardNumber must have 9 digits") String cardNumber,
    @NotNull @Past LocalDate birthDate) {}
