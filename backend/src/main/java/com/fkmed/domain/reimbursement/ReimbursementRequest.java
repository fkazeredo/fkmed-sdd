package com.fkmed.domain.reimbursement;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/** Reimbursement request aggregate (SPEC-0015/0016). */
@Entity
@Table(name = "reimbursement_request")
@Getter
public class ReimbursementRequest {

  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private String protocol;

  @Column(name = "beneficiary_id", nullable = false)
  private UUID beneficiaryId;

  @Column(name = "expense_type_code", nullable = false)
  private String expenseTypeCode;

  @Column(name = "care_date", nullable = false)
  private LocalDate careDate;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(name = "provider_name", nullable = false)
  private String providerName;

  @Column(name = "provider_council_code", nullable = false)
  private String providerCouncilCode;

  @Column(name = "provider_council_number", nullable = false)
  private String providerCouncilNumber;

  @Column(name = "provider_council_uf", nullable = false)
  private String providerCouncilUf;

  @Column(name = "provider_document", nullable = false)
  private String providerDocument;

  @Column(name = "provider_specialty", nullable = false)
  private String providerSpecialty;

  @Column(name = "bank_code", nullable = false)
  private String bankCode;

  @Column(name = "bank_agency", nullable = false)
  private String bankAgency;

  @Column(name = "bank_account", nullable = false)
  private String bankAccount;

  @Column(name = "bank_account_digit", nullable = false)
  private String bankAccountDigit;

  @Enumerated(EnumType.STRING)
  @Column(name = "bank_account_type", nullable = false)
  private BankAccountType bankAccountType;

  @Column(name = "term_version", nullable = false)
  private String termVersion;

  @Column(name = "term_accepted_at", nullable = false)
  private Instant termAcceptedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReimbursementStatus status;

  @Column(name = "expected_payment_date", nullable = false)
  private LocalDate expectedPaymentDate;

  @Column(name = "amount_reimbursed")
  private BigDecimal amountReimbursed;

  @Column(name = "glosa_amount")
  private BigDecimal glosaAmount;

  @Column(name = "glosa_reason")
  private String glosaReason;

  @Column(name = "denial_reason")
  private String denialReason;

  @Column(name = "pendency_description")
  private String pendencyDescription;

  @Column(name = "pendency_opened_at")
  private Instant pendencyOpenedAt;

  @Column(name = "pendency_deadline_at")
  private LocalDate pendencyDeadlineAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "payment_failed_at")
  private Instant paymentFailedAt;

  @Column(name = "payment_failure_reason")
  private String paymentFailureReason;

  @Column(name = "idempotency_key", nullable = false, unique = true)
  private String idempotencyKey;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @OneToMany(
      mappedBy = "request",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("sessionDate asc")
  private List<SessionItem> sessionItems = new ArrayList<>();

  @OneToMany(
      mappedBy = "request",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<ReimbursementDocument> documents = new ArrayList<>();

  @OneToMany(
      mappedBy = "request",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("occurredAt asc")
  private List<TimelineEvent> timelineEvents = new ArrayList<>();

  /** JPA only. */
  protected ReimbursementRequest() {}

  static ReimbursementRequest submit(
      SubmitReimbursementCommand command,
      String protocol,
      AdhesionTerm term,
      LocalDate expectedPaymentDate,
      UUID createdBy,
      Instant now) {
    Objects.requireNonNull(command, "command is required");
    Objects.requireNonNull(protocol, "protocol is required");
    Objects.requireNonNull(term, "term is required");
    Objects.requireNonNull(expectedPaymentDate, "expectedPaymentDate is required");
    Objects.requireNonNull(createdBy, "createdBy is required");

    ReimbursementRequest request = new ReimbursementRequest();
    request.id = UUID.randomUUID();
    request.protocol = protocol;
    request.beneficiaryId = command.beneficiaryId();
    request.expenseTypeCode = command.expenseTypeCode();
    request.careDate = command.careDate();
    request.amount = command.amount();
    request.providerName = command.providerName();
    request.providerCouncilCode = command.providerCouncilCode();
    request.providerCouncilNumber = command.providerCouncilNumber();
    request.providerCouncilUf = command.providerCouncilUf();
    request.providerDocument = command.providerDocument();
    request.providerSpecialty = command.providerSpecialty();
    request.bankCode = command.bankCode();
    request.bankAgency = command.bankAgency();
    request.bankAccount = command.bankAccount();
    request.bankAccountDigit = command.bankAccountDigit();
    request.bankAccountType = command.bankAccountType();
    request.termVersion = term.getVersion();
    request.termAcceptedAt = now;
    request.status = ReimbursementStatus.EM_ANALISE;
    request.expectedPaymentDate = expectedPaymentDate;
    request.idempotencyKey = command.idempotencyKey();
    request.createdBy = createdBy;
    request.createdAt = now;
    request.addSessions(command.sessions());
    request.addDocuments(command.documents(), now);
    request.timelineEvents.add(
        TimelineEvent.of(
            request, now, ReimbursementStatus.EM_ANALISE, "Solicitacao recebida - em analise."));
    return request;
  }

  void markProcessing(ReimbursementCalculation calculation, LocalDate expectedDate, Instant now) {
    requireTransition(ReimbursementStatus.PROCESSAMENTO);
    applyCalculation(calculation);
    expectedPaymentDate = expectedDate;
    pendencyDescription = null;
    pendencyOpenedAt = null;
    pendencyDeadlineAt = null;
    transition(
        ReimbursementStatus.PROCESSAMENTO,
        now,
        "Documentacao completa. Reembolso em processamento.");
  }

  void openPendency(String description, LocalDate deadline, Instant now) {
    requireTransition(ReimbursementStatus.PENDENTE_DOCUMENTACAO);
    pendencyDescription = requiredText(description);
    pendencyOpenedAt = now;
    pendencyDeadlineAt = deadline;
    transition(ReimbursementStatus.PENDENTE_DOCUMENTACAO, now, pendencyDescription);
  }

  void resolvePendency(
      List<UploadedDocument> newDocuments,
      ReimbursementCalculation calculation,
      LocalDate expectedDate,
      Instant now) {
    if (status != ReimbursementStatus.PENDENTE_DOCUMENTACAO) {
      throw new ReimbursementPendencyNotOpenException();
    }
    addDocuments(newDocuments, now);
    applyCalculation(calculation);
    expectedPaymentDate = expectedDate;
    pendencyDescription = null;
    pendencyOpenedAt = null;
    pendencyDeadlineAt = null;
    transition(
        ReimbursementStatus.PROCESSAMENTO,
        now,
        "Documentacao recebida. Reembolso em processamento.");
  }

  void approve(Instant now) {
    requireTransition(ReimbursementStatus.APROVADO);
    transition(ReimbursementStatus.APROVADO, now, "Reembolso aprovado.");
  }

  void deny(String reason, Instant now) {
    requireTransition(ReimbursementStatus.NEGADO);
    denialReason = requiredText(reason);
    amountReimbursed = BigDecimal.ZERO;
    transition(ReimbursementStatus.NEGADO, now, denialReason);
  }

  void cancelByPendencyExpiry(Instant now) {
    if (status != ReimbursementStatus.PENDENTE_DOCUMENTACAO) {
      throw new ReimbursementInvalidTransitionException();
    }
    transition(
        ReimbursementStatus.CANCELADO,
        now,
        "Solicitacao cancelada por falta de resposta a pendencia em 30 dias.");
  }

  boolean pay(Instant now) {
    if (status == ReimbursementStatus.PAGO) {
      return false;
    }
    requireTransition(ReimbursementStatus.PAGO);
    paidAt = now;
    paymentFailureReason = null;
    paymentFailedAt = null;
    transition(ReimbursementStatus.PAGO, now, "Credito realizado na conta informada.");
    return true;
  }

  void failPayment(String reason, Instant now) {
    requireTransition(ReimbursementStatus.PAGAMENTO_NAO_EFETUADO);
    paymentFailureReason =
        reason == null || reason.isBlank() ? "Falha no credito bancario." : reason.strip();
    paymentFailedAt = now;
    transition(
        ReimbursementStatus.PAGAMENTO_NAO_EFETUADO, now, "Nao foi possivel creditar o valor.");
  }

  void correctBankAndPay(
      String bankCode,
      String bankAgency,
      String bankAccount,
      String bankAccountDigit,
      BankAccountType bankAccountType,
      LocalDate expectedDate,
      Instant now) {
    if (status != ReimbursementStatus.PAGAMENTO_NAO_EFETUADO) {
      throw new ReimbursementCorrectionNotAllowedException();
    }
    this.bankCode = bankCode;
    this.bankAgency = bankAgency;
    this.bankAccount = bankAccount;
    this.bankAccountDigit = bankAccountDigit;
    this.bankAccountType = bankAccountType;
    this.expectedPaymentDate = expectedDate;
    pay(now);
  }

  String maskedBankAccount() {
    String account = bankAccount == null ? "" : bankAccount;
    String suffix = account.length() <= 4 ? account : account.substring(account.length() - 4);
    return "..." + suffix;
  }

  private void applyCalculation(ReimbursementCalculation calculation) {
    amountReimbursed = calculation.amountReimbursed();
    glosaAmount = calculation.glosaAmount();
    glosaReason = calculation.glosaReason();
  }

  private void addSessions(List<SessionInput> sessions) {
    for (SessionInput session : sessions == null ? List.<SessionInput>of() : sessions) {
      sessionItems.add(SessionItem.of(this, session.sessionDate(), session.amount()));
    }
  }

  private void addDocuments(List<UploadedDocument> uploaded, Instant now) {
    for (UploadedDocument document : uploaded == null ? List.<UploadedDocument>of() : uploaded) {
      documents.add(
          ReimbursementDocument.of(
              this,
              document.category(),
              document.content(),
              document.contentType(),
              document.fileName(),
              now));
    }
  }

  private void transition(ReimbursementStatus target, Instant now, String description) {
    status = target;
    timelineEvents.add(TimelineEvent.of(this, now, target, description));
  }

  private void requireTransition(ReimbursementStatus target) {
    boolean allowed =
        switch (status) {
          case EM_ANALISE ->
              target == ReimbursementStatus.PROCESSAMENTO
                  || target == ReimbursementStatus.PENDENTE_DOCUMENTACAO
                  || target == ReimbursementStatus.NEGADO;
          case PENDENTE_DOCUMENTACAO ->
              target == ReimbursementStatus.PROCESSAMENTO
                  || target == ReimbursementStatus.CANCELADO;
          case PROCESSAMENTO ->
              target == ReimbursementStatus.APROVADO
                  || target == ReimbursementStatus.PENDENTE_DOCUMENTACAO
                  || target == ReimbursementStatus.NEGADO;
          case APROVADO ->
              target == ReimbursementStatus.PAGO
                  || target == ReimbursementStatus.PAGAMENTO_NAO_EFETUADO;
          case PAGAMENTO_NAO_EFETUADO -> target == ReimbursementStatus.PAGO;
          case PAGO, NEGADO, CANCELADO -> false;
        };
    if (!allowed) {
      throw new ReimbursementInvalidTransitionException();
    }
  }

  private static String requiredText(String value) {
    if (value == null || value.isBlank()) {
      throw new ReimbursementInvalidTransitionException();
    }
    return value.strip();
  }
}
