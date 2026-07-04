package com.fkmed.domain.plan;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Minimal, non-sensitive view of a beneficiary exposed across module boundaries by {@link
 * Beneficiaries} (SPEC-0002 first access). Deliberately omits CPF/CNS — the identity module needs
 * only the id (to link an account), the card (token binding) and the role/birth date (BR3 age
 * check); sensitive fields never leave the plan module (SPEC-0003 BR8 masking posture).
 *
 * @param id the beneficiary's stable id (stored by other contexts as a value, never as a FK).
 * @param cardNumber the 9-digit plan card number.
 * @param birthDate the beneficiary's birth date.
 * @param role titular or dependent.
 */
public record BeneficiaryMatch(
    UUID id, String cardNumber, LocalDate birthDate, BeneficiaryRole role) {}
