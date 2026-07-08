package com.fkmed.domain.reimbursement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Detail payload for SPEC-0016. */
public record ReimbursementDetailView(
    UUID id,
    String protocol,
    String expenseType,
    String beneficiary,
    ReimbursementStatus status,
    LocalDate careDate,
    Instant requestedAt,
    LocalDate expectedPaymentDate,
    BigDecimal amountRequested,
    BigDecimal amountReimbursed,
    GlosaView glosa,
    String denialReason,
    PendencyView pendency,
    BankView bank,
    ProviderView provider,
    List<DocumentView> documents,
    List<TimelineView> timeline,
    String regulatoryNote) {

  public record GlosaView(BigDecimal amount, String reason) {}

  public record PendencyView(String description, LocalDate deadlineAt) {}

  public record BankView(String bankCode, String agency, String account, BankAccountType type) {}

  public record ProviderView(
      String name, String councilCode, String councilNumber, String councilUf, String specialty) {}

  public record DocumentView(
      DocumentCategory category, String fileName, int fileSize, Instant uploadedAt) {}

  public record TimelineView(Instant occurredAt, ReimbursementStatus status, String description) {}
}
