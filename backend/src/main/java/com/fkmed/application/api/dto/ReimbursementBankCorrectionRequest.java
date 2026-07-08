package com.fkmed.application.api.dto;

import com.fkmed.domain.reimbursement.BankAccountType;
import com.fkmed.domain.reimbursement.BankCorrectionCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body of POST /api/reimbursements/{id}/bank-correction. */
public record ReimbursementBankCorrectionRequest(
    @NotBlank String bankCode,
    @NotBlank String bankAgency,
    @NotBlank String bankAccount,
    @NotBlank String bankAccountDigit,
    @NotNull BankAccountType bankAccountType) {

  public BankCorrectionCommand toCommand() {
    return new BankCorrectionCommand(
        bankCode, bankAgency, bankAccount, bankAccountDigit, bankAccountType);
  }
}
