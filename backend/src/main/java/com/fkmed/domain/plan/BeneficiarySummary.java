package com.fkmed.domain.plan;

import java.util.UUID;

/**
 * Card summary of a single accessible beneficiary (SPEC-0003 §API Contracts + SPEC-0005 BR1
 * beneficiary card). Built only after the family-scope check passes; carries no CPF/CNS/bank data
 * (BR8). {@code avatarUrl} points at the beneficiary's photo endpoint when a photo is set, else
 * {@code null} so the client shows the placeholder (SPEC-0006 BR3).
 */
public record BeneficiarySummary(
    UUID beneficiaryId,
    String firstName,
    String fullName,
    BeneficiaryRole role,
    String planName,
    String cardNumber,
    String avatarUrl) {}
