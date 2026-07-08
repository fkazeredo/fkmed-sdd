package com.fkmed.domain.reimbursement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A fully-formed reimbursement submission (SPEC-0015 steps 2-6), already resolved to the
 * beneficiary/provider/bank/documents fields — built by {@link ReimbursementService#submit} from
 * the controller's request, then validated and turned into a {@link ReimbursementRequest}.
 */
public record SubmitReimbursementCommand(
    UUID beneficiaryId,
    String expenseTypeCode,
    LocalDate careDate,
    BigDecimal amount,
    List<SessionInput> sessions,
    String providerName,
    String providerCouncilCode,
    String providerCouncilNumber,
    String providerCouncilUf,
    String providerDocument,
    String providerSpecialty,
    String bankCode,
    String bankAgency,
    String bankAccount,
    String bankAccountDigit,
    BankAccountType bankAccountType,
    String termVersion,
    List<UploadedDocument> documents,
    String idempotencyKey) {}
