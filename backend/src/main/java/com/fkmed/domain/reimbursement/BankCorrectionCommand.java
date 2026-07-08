package com.fkmed.domain.reimbursement;

/** Bank data correction payload (SPEC-0016 BR8). */
public record BankCorrectionCommand(
    String bankCode,
    String bankAgency,
    String bankAccount,
    String bankAccountDigit,
    BankAccountType bankAccountType) {}
