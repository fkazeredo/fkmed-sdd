package com.fkmed.application.api.dto;

import com.fkmed.domain.reimbursement.BankAccountType;
import com.fkmed.domain.reimbursement.SubmitReimbursementCommand;
import com.fkmed.domain.reimbursement.UploadedDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** JSON part of {@code POST /api/reimbursements} (SPEC-0015 steps 2-6). */
public record ReimbursementSubmitRequest(
    @NotNull UUID beneficiaryId,
    @NotBlank String expenseTypeCode,
    @NotNull LocalDate careDate,
    @NotNull BigDecimal amount,
    List<@Valid ReimbursementSessionRequest> sessions,
    @NotBlank String providerName,
    @NotBlank String providerCouncilCode,
    @NotBlank String providerCouncilNumber,
    @NotBlank String providerCouncilUf,
    @NotBlank String providerDocument,
    @NotBlank String providerSpecialty,
    @NotBlank String bankCode,
    @NotBlank String bankAgency,
    @NotBlank String bankAccount,
    @NotBlank String bankAccountDigit,
    @NotNull BankAccountType bankAccountType,
    @NotBlank String acceptedTermVersion,
    List<@Valid ReimbursementDocumentRequest> documents) {

  public SubmitReimbursementCommand toCommand(
      String idempotencyKey, List<UploadedDocument> uploadedDocuments) {
    return new SubmitReimbursementCommand(
        beneficiaryId,
        expenseTypeCode,
        careDate,
        amount,
        sessions == null
            ? List.of()
            : sessions.stream().map(ReimbursementSessionRequest::toInput).toList(),
        providerName,
        providerCouncilCode,
        providerCouncilNumber,
        providerCouncilUf,
        providerDocument,
        providerSpecialty,
        bankCode,
        bankAgency,
        bankAccount,
        bankAccountDigit,
        bankAccountType,
        acceptedTermVersion,
        uploadedDocuments,
        idempotencyKey);
  }
}
