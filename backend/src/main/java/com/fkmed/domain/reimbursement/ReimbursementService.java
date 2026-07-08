package com.fkmed.domain.reimbursement;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.BeneficiaryNotAccessibleException;
import com.fkmed.domain.plan.ProtocolGenerator;
import com.fkmed.domain.upload.FileContentType;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service and public facade of the reimbursement module (SPEC-0015/0016). */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReimbursementService {

  private static final String PROTOCOL_PREFIX = "RE";
  private static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
  private static final int MAX_TOTAL_BYTES = 20 * 1024 * 1024;
  private static final int CONSULTA_BUSINESS_DAYS = 5;
  private static final int DEFAULT_BUSINESS_DAYS = 10;
  private static final int PENDENCY_DAYS = 30;
  private static final String REGULATORY_NOTE =
      "Prazo regulatorio: ate 30 dias corridos apos a documentacao completa.";

  private final ReimbursementRequestRepository requests;
  private final ExpenseTypeRepository expenseTypes;
  private final ProfessionalCouncilRepository councils;
  private final BankRepository banks;
  private final ReimbursementTableRepository table;
  private final AdhesionTermRepository terms;
  private final BeneficiaryAccess beneficiaryAccess;
  private final ProtocolGenerator protocolGenerator;
  private final AuditRecorder auditRecorder;
  private final ApplicationEventPublisher events;
  private final MeterRegistry metrics;
  private final Clock clock;

  public EligibilityView eligibility(String callerCard) {
    return new EligibilityView(beneficiaryAccess.reimbursementEligible(callerCard));
  }

  public AdhesionTermView term(String callerCard) {
    requireEligible(callerCard);
    AdhesionTerm term = currentTerm();
    return new AdhesionTermView(term.getVersion(), term.getPublishedAt(), term.getBody());
  }

  public CatalogView catalog(String callerCard) {
    requireEligible(callerCard);
    return new CatalogView(
        expenseTypes.findAll(Sort.by("name")).stream()
            .map(type -> new CatalogView.ExpenseTypeView(type.getCode(), type.getName()))
            .toList(),
        councils.findAll(Sort.by("code")).stream()
            .map(
                council ->
                    new CatalogView.ProfessionalCouncilView(council.getCode(), council.getName()))
            .toList(),
        banks.findAll(Sort.by("code")).stream()
            .map(bank -> new CatalogView.BankView(bank.getCode(), bank.getName()))
            .toList());
  }

  public DocumentationGuideView documentationGuide(String callerCard, String expenseTypeCode) {
    requireEligible(callerCard);
    String code = normalizeCode(expenseTypeCode);
    return new DocumentationGuideView(code, documentationItems(code));
  }

  @Transactional
  public ReimbursementSubmissionResult submit(
      String callerCard,
      UUID authorAccountId,
      SubmitReimbursementCommand command,
      AuditContext auditContext) {
    requireEligible(callerCard);
    validateIdempotencyKey(command.idempotencyKey());
    beneficiaryAccess.requireAccessible(callerCard, command.beneficiaryId());
    var existing = requests.findByIdempotencyKey(command.idempotencyKey());
    if (existing.isPresent()) {
      if (!existing.get().getBeneficiaryId().equals(command.beneficiaryId())) {
        throw new BeneficiaryNotAccessibleException();
      }
      return submissionResultOf(existing.get());
    }

    validateContacts(callerCard, command.beneficiaryId());
    String expenseType = validateExpenseType(command.expenseTypeCode());
    ReimbursementTableEntry tableEntry = requireTableEntry(expenseType);
    AdhesionTerm term = validateTerm(command.termVersion());
    validateDates(command.careDate());
    validateAmount(command.amount());
    validateSessions(tableEntry, command.sessions(), command.amount());
    validateProvider(command);
    validateBank(
        command.bankCode(),
        command.bankAgency(),
        command.bankAccount(),
        command.bankAccountDigit(),
        command.bankAccountType());
    List<UploadedDocument> documents =
        validateDocuments(
            expenseType,
            command.documents(),
            reimbursementCategories(),
            requiredReimbursementCategories(expenseType),
            ReimbursementDocumentRequiredException::new);

    Instant now = clock.instant();
    LocalDate expectedPaymentDate = expectedDate(expenseType);
    String protocol = protocolGenerator.next(PROTOCOL_PREFIX);
    SubmitReimbursementCommand validated = withValidatedDocuments(command, expenseType, documents);
    ReimbursementRequest request =
        ReimbursementRequest.submit(
            validated, protocol, term, expectedPaymentDate, authorAccountId, now);
    request.markProcessing(
        calculate(tableEntry, request.getAmount(), request.getSessionItems()),
        expectedPaymentDate,
        now);
    requests.save(request);
    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.REIMBURSEMENT_SUBMITTED,
            authorAccountId,
            command.beneficiaryId(),
            Map.of(
                "protocol", protocol,
                "expenseType", expenseType,
                "amount", command.amount().toPlainString()),
            auditContext));
    metrics.counter("reimbursement.submitted", "type", expenseType).increment();
    log.info(
        "reimbursement {} submitted for beneficiary {}", protocol, mask(command.beneficiaryId()));
    events.publishEvent(
        new ReimbursementSubmitted(
            request.getId(),
            request.getBeneficiaryId(),
            request.getProtocol(),
            request.getExpenseTypeCode(),
            request.getAmount(),
            request.getExpectedPaymentDate()));
    return submissionResultOf(request);
  }

  public List<ReimbursementHistoryItem> history(
      String callerCard,
      UUID beneficiaryId,
      ReimbursementStatus status,
      LocalDate from,
      LocalDate to) {
    requireEligible(callerCard);
    Map<UUID, String> names = accessibleNames(callerCard);
    List<UUID> ids = targetIds(callerCard, beneficiaryId);
    Predicate<ReimbursementRequest> filter =
        request ->
            (status == null || request.getStatus() == status)
                && within(request.getCreatedAt(), from, to, clock.getZone());
    return requests.findByBeneficiaryIdInOrderByCreatedAtDesc(ids).stream()
        .filter(filter)
        .map(request -> historyItemOf(request, names))
        .toList();
  }

  public ReimbursementDetailView detail(String callerCard, UUID id) {
    requireEligible(callerCard);
    Map<UUID, String> names = accessibleNames(callerCard);
    ReimbursementRequest request = scopedRequest(callerCard, id);
    return detailOf(request, names);
  }

  public ReimbursementStatementView statement(
      String callerCard, UUID beneficiaryId, LocalDate from, LocalDate to) {
    requireEligible(callerCard);
    Map<UUID, String> names = accessibleNames(callerCard);
    List<UUID> ids = targetIds(callerCard, beneficiaryId);
    List<ReimbursementStatementView.StatementItem> items =
        requests.findByBeneficiaryIdInOrderByCreatedAtDesc(ids).stream()
            .filter(request -> request.getStatus() == ReimbursementStatus.PAGO)
            .filter(request -> within(request.getPaidAt(), from, to, clock.getZone()))
            .map(
                request ->
                    new ReimbursementStatementView.StatementItem(
                        request.getId(),
                        request.getProtocol(),
                        names.getOrDefault(request.getBeneficiaryId(), "Beneficiario"),
                        request.getPaidAt(),
                        money(request.getAmountReimbursed())))
            .toList();
    BigDecimal total =
        items.stream()
            .map(ReimbursementStatementView.StatementItem::amountPaid)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new ReimbursementStatementView(items, total);
  }

  @Transactional
  public ReimbursementActionResult resolvePendency(
      String callerCard, UUID id, List<UploadedDocument> documents) {
    requireEligible(callerCard);
    ReimbursementRequest request = scopedRequest(callerCard, id);
    List<UploadedDocument> validated =
        validateDocuments(
            request.getExpenseTypeCode(),
            documents,
            reimbursementCategories(),
            EnumSet.noneOf(DocumentCategory.class),
            ReimbursementDocumentRequiredException::new);
    if (validated.isEmpty()) {
      throw new ReimbursementDocumentRequiredException();
    }
    ReimbursementTableEntry tableEntry = requireTableEntry(request.getExpenseTypeCode());
    Instant now = clock.instant();
    request.resolvePendency(
        validated,
        calculate(tableEntry, request.getAmount(), request.getSessionItems()),
        expectedDate(request.getExpenseTypeCode()),
        now);
    events.publishEvent(statusEvent(request, null, now));
    metrics.counter("reimbursement.transition", "status", request.getStatus().name()).increment();
    return ReimbursementActionResult.of(request);
  }

  @Transactional
  public ReimbursementActionResult correctBank(
      String callerCard, UUID id, BankCorrectionCommand command) {
    requireEligible(callerCard);
    ReimbursementRequest request = scopedRequest(callerCard, id);
    validateBank(
        command.bankCode(),
        command.bankAgency(),
        command.bankAccount(),
        command.bankAccountDigit(),
        command.bankAccountType());
    Instant now = clock.instant();
    request.correctBankAndPay(
        normalizeCode(command.bankCode()),
        command.bankAgency(),
        command.bankAccount(),
        command.bankAccountDigit(),
        command.bankAccountType(),
        expectedDate(request.getExpenseTypeCode()),
        now);
    events.publishEvent(statusEvent(request, null, now));
    metrics.counter("reimbursement.transition", "status", request.getStatus().name()).increment();
    return ReimbursementActionResult.of(request);
  }

  @Transactional
  public ReimbursementActionResult approve(UUID id) {
    ReimbursementRequest request =
        requests.findById(id).orElseThrow(ReimbursementNotFoundException::new);
    Instant now = clock.instant();
    request.approve(now);
    events.publishEvent(statusEvent(request, request.getGlosaReason(), now));
    metrics.counter("reimbursement.transition", "status", request.getStatus().name()).increment();
    return ReimbursementActionResult.of(request);
  }

  @Transactional
  public ReimbursementActionResult deny(UUID id, String reason) {
    ReimbursementRequest request =
        requests.findById(id).orElseThrow(ReimbursementNotFoundException::new);
    Instant now = clock.instant();
    request.deny(reason, now);
    events.publishEvent(statusEvent(request, reason, now));
    metrics.counter("reimbursement.transition", "status", request.getStatus().name()).increment();
    return ReimbursementActionResult.of(request);
  }

  @Transactional
  public ReimbursementActionResult openPendency(UUID id, String description) {
    ReimbursementRequest request =
        requests.findById(id).orElseThrow(ReimbursementNotFoundException::new);
    Instant now = clock.instant();
    request.openPendency(description, LocalDate.now(clock).plusDays(PENDENCY_DAYS), now);
    events.publishEvent(statusEvent(request, description, now));
    metrics.counter("reimbursement.transition", "status", request.getStatus().name()).increment();
    return ReimbursementActionResult.of(request);
  }

  @Transactional
  public ReimbursementActionResult pay(UUID id, boolean success, String failureReason) {
    ReimbursementRequest request =
        requests.findById(id).orElseThrow(ReimbursementNotFoundException::new);
    Instant now = clock.instant();
    boolean changed = true;
    if (success) {
      changed = request.pay(now);
    } else {
      request.failPayment(failureReason, now);
    }
    if (changed) {
      events.publishEvent(statusEvent(request, success ? null : failureReason, now));
      metrics.counter("reimbursement.transition", "status", request.getStatus().name()).increment();
    }
    return ReimbursementActionResult.of(request);
  }

  @Transactional
  public int cancelExpiredPendencies() {
    Instant now = clock.instant();
    List<ReimbursementRequest> expired =
        requests.findByStatusAndPendencyDeadlineAtBefore(
            ReimbursementStatus.PENDENTE_DOCUMENTACAO, LocalDate.now(clock));
    for (ReimbursementRequest request : expired) {
      request.cancelByPendencyExpiry(now);
      events.publishEvent(statusEvent(request, null, now));
    }
    if (!expired.isEmpty()) {
      metrics.counter("reimbursement.pendency.auto-cancelled").increment(expired.size());
    }
    return expired.size();
  }

  ReimbursementCalculation calculate(
      ReimbursementTableEntry tableEntry, BigDecimal total, List<SessionItem> sessions) {
    BigDecimal reimbursed;
    if (tableEntry.isPerSession()) {
      reimbursed =
          sessions.stream()
              .map(session -> cap(session.getAmount(), tableEntry))
              .reduce(BigDecimal.ZERO, BigDecimal::add);
    } else {
      reimbursed = cap(total, tableEntry);
    }
    return ReimbursementCalculation.of(money(reimbursed), money(total));
  }

  List<UploadedDocument> validatePreviewDocuments(List<UploadedDocument> documents) {
    return validateDocuments(
        ExpenseTypeCodes.EXAME,
        documents,
        EnumSet.of(DocumentCategory.BUDGET, DocumentCategory.MEDICAL_ORDER),
        EnumSet.of(DocumentCategory.BUDGET, DocumentCategory.MEDICAL_ORDER),
        PreviewAttachmentsRequiredException::new);
  }

  ReimbursementTableEntry requireTableEntry(String expenseType) {
    return table
        .findById(expenseType)
        .orElseThrow(
            () -> new IllegalStateException("missing reimbursement table row: " + expenseType));
  }

  void requireEligible(String callerCard) {
    if (!beneficiaryAccess.reimbursementEligible(callerCard)) {
      throw new ReimbursementNotEligibleException();
    }
  }

  String validateExpenseType(String value) {
    String code = normalizeCode(value);
    if (!expenseTypes.existsById(code)) {
      throw new ReimbursementAmountInvalidException();
    }
    return code;
  }

  List<UUID> targetIds(String callerCard, UUID beneficiaryId) {
    if (beneficiaryId != null) {
      beneficiaryAccess.requireAccessible(callerCard, beneficiaryId);
      return List.of(beneficiaryId);
    }
    return beneficiaryAccess.accessibleFor(callerCard).stream()
        .map(AccessibleBeneficiary::beneficiaryId)
        .toList();
  }

  Map<UUID, String> accessibleNames(String callerCard) {
    return beneficiaryAccess.accessibleFor(callerCard).stream()
        .collect(
            Collectors.toMap(
                AccessibleBeneficiary::beneficiaryId, AccessibleBeneficiary::firstName));
  }

  private ReimbursementRequest scopedRequest(String callerCard, UUID id) {
    List<UUID> ids = targetIds(callerCard, null);
    return requests
        .findByIdAndBeneficiaryIdIn(id, ids)
        .orElseThrow(ReimbursementNotFoundException::new);
  }

  private AdhesionTerm currentTerm() {
    return terms.findAll(Sort.by(Sort.Direction.DESC, "publishedAt")).stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("reimbursement term seed is missing"));
  }

  private void validateContacts(String callerCard, UUID beneficiaryId) {
    if (!beneficiaryAccess.hasRequiredContacts(callerCard, beneficiaryId)) {
      throw new ReimbursementContactsMissingException();
    }
  }

  private AdhesionTerm validateTerm(String acceptedVersion) {
    AdhesionTerm term = currentTerm();
    if (acceptedVersion == null || !term.getVersion().equals(acceptedVersion.strip())) {
      throw new ReimbursementTermNotAcceptedException();
    }
    return term;
  }

  private void validateDates(LocalDate careDate) {
    LocalDate today = LocalDate.now(clock);
    if (careDate == null || careDate.isAfter(today)) {
      throw new ReimbursementCareDateInvalidException();
    }
    if (careDate.isBefore(today.minusMonths(12))) {
      metrics.counter("reimbursement.blocked", "reason", "deadline").increment();
      throw new ReimbursementDeadlineExpiredException();
    }
  }

  private void validateAmount(BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) {
      throw new ReimbursementAmountInvalidException();
    }
  }

  private void validateSessions(
      ReimbursementTableEntry tableEntry, List<SessionInput> sessions, BigDecimal total) {
    List<SessionInput> provided = sessions == null ? List.of() : sessions;
    if (!tableEntry.isPerSession()) {
      if (!provided.isEmpty()) {
        throw new ReimbursementSessionsSumMismatchException();
      }
      return;
    }
    if (provided.isEmpty()) {
      throw new ReimbursementSessionsSumMismatchException();
    }
    BigDecimal sum = BigDecimal.ZERO;
    for (SessionInput session : provided) {
      validateDates(session.sessionDate());
      validateAmount(session.amount());
      sum = sum.add(session.amount());
    }
    if (sum.compareTo(total) != 0) {
      throw new ReimbursementSessionsSumMismatchException();
    }
  }

  private void validateProvider(SubmitReimbursementCommand command) {
    if (isBlank(command.providerName())
        || command.providerName().length() > 140
        || !councils.existsById(normalizeCode(command.providerCouncilCode()))
        || command.providerCouncilNumber() == null
        || !command.providerCouncilNumber().matches("\\d{1,10}")
        || !normalizeCode(command.providerCouncilUf()).matches("[A-Z]{2}")
        || !DocumentCheckDigits.isValid(onlyDigits(command.providerDocument()))
        || isBlank(command.providerSpecialty())
        || command.providerSpecialty().length() > 140) {
      throw new ReimbursementProviderInvalidException();
    }
  }

  private void validateBank(
      String bankCode,
      String bankAgency,
      String bankAccount,
      String bankAccountDigit,
      BankAccountType bankAccountType) {
    if (!banks.existsById(normalizeCode(bankCode))
        || bankAgency == null
        || !bankAgency.matches("\\d{4}")
        || bankAccount == null
        || !bankAccount.matches("\\d{1,20}")
        || bankAccountDigit == null
        || !bankAccountDigit.matches("\\d{1,2}")
        || bankAccountType == null) {
      throw new ReimbursementBankAccountNotAllowedException();
    }
  }

  private List<UploadedDocument> validateDocuments(
      String expenseType,
      List<UploadedDocument> documents,
      EnumSet<DocumentCategory> allowed,
      EnumSet<DocumentCategory> required,
      java.util.function.Supplier<? extends RuntimeException> missingException) {
    List<UploadedDocument> provided = documents == null ? List.of() : documents;
    int total = 0;
    java.util.ArrayList<UploadedDocument> detected = new java.util.ArrayList<>();
    for (UploadedDocument document : provided) {
      if (document.content() == null
          || document.content().length == 0
          || document.category() == null
          || !allowed.contains(document.category())) {
        throw missingException.get();
      }
      if (document.content().length > MAX_FILE_BYTES) {
        metrics.counter("reimbursement.upload.rejected", "reason", "file-size").increment();
        throw new ReimbursementDocumentTooLargeException();
      }
      total += document.content().length;
      if (total > MAX_TOTAL_BYTES) {
        metrics.counter("reimbursement.upload.rejected", "reason", "total-size").increment();
        throw new ReimbursementTotalSizeExceededException();
      }
      String contentType =
          FileContentType.detect(document.content())
              .orElseThrow(ReimbursementDocumentInvalidContentException::new);
      detected.add(
          new UploadedDocument(
              document.category(),
              document.content(),
              contentType,
              safeFileName(document.fileName())));
    }
    for (DocumentCategory category : required) {
      if (detected.stream().noneMatch(document -> document.category() == category)) {
        throw missingException.get();
      }
    }
    return detected;
  }

  private EnumSet<DocumentCategory> requiredReimbursementCategories(String expenseType) {
    EnumSet<DocumentCategory> required = EnumSet.of(DocumentCategory.RECEIPT);
    if (ExpenseTypeCodes.requiresMedicalOrder(expenseType)) {
      required.add(DocumentCategory.MEDICAL_ORDER);
    }
    return required;
  }

  private static EnumSet<DocumentCategory> reimbursementCategories() {
    return EnumSet.of(
        DocumentCategory.RECEIPT, DocumentCategory.MEDICAL_ORDER, DocumentCategory.COMPLEMENTARY);
  }

  private List<String> documentationItems(String expenseType) {
    return switch (expenseType) {
      case ExpenseTypeCodes.CONSULTA ->
          List.of(
              "Nota fiscal ou recibo com nome do paciente",
              "Descricao do servico, data e valor pago",
              "Identificacao do profissional: nome, conselho, CPF/CNPJ, endereco e assinatura/carimbo");
      case ExpenseTypeCodes.EXAME ->
          List.of(
              "Nota fiscal ou recibo com nome do paciente, data, descricao e valor",
              "Pedido medico do exame",
              "Identificacao do prestador com conselho profissional e CPF/CNPJ");
      case ExpenseTypeCodes.TERAPIA, ExpenseTypeCodes.PSICOLOGIA ->
          List.of(
              "Recibo com datas e valores por sessao",
              "Pedido ou relatorio do profissional solicitante",
              "Identificacao do prestador com conselho profissional e CPF/CNPJ");
      case ExpenseTypeCodes.HONORARIOS ->
          List.of(
              "Nota fiscal ou recibo por profissional",
              "Relatorio medico do procedimento",
              "Boletim anestesico quando aplicavel");
      default ->
          List.of(
              "Nota fiscal ou recibo",
              "Relatorio justificando a despesa",
              "Identificacao do prestador com CPF/CNPJ");
    };
  }

  private SubmitReimbursementCommand withValidatedDocuments(
      SubmitReimbursementCommand command, String expenseType, List<UploadedDocument> documents) {
    return new SubmitReimbursementCommand(
        command.beneficiaryId(),
        expenseType,
        command.careDate(),
        command.amount(),
        command.sessions() == null
            ? List.of()
            : command.sessions().stream()
                .sorted(Comparator.comparing(SessionInput::sessionDate))
                .toList(),
        command.providerName().strip(),
        normalizeCode(command.providerCouncilCode()),
        command.providerCouncilNumber(),
        normalizeCode(command.providerCouncilUf()),
        onlyDigits(command.providerDocument()),
        command.providerSpecialty().strip(),
        normalizeCode(command.bankCode()),
        command.bankAgency(),
        command.bankAccount(),
        command.bankAccountDigit(),
        command.bankAccountType(),
        command.termVersion(),
        documents,
        command.idempotencyKey().strip());
  }

  private ReimbursementHistoryItem historyItemOf(
      ReimbursementRequest request, Map<UUID, String> names) {
    return new ReimbursementHistoryItem(
        request.getId(),
        request.getProtocol(),
        request.getExpenseTypeCode(),
        names.getOrDefault(request.getBeneficiaryId(), "Beneficiario"),
        request.getCreatedAt(),
        request.getAmount(),
        request.getAmountReimbursed(),
        request.getStatus());
  }

  private ReimbursementDetailView detailOf(ReimbursementRequest request, Map<UUID, String> names) {
    BigDecimal glosa = money(request.getGlosaAmount());
    return new ReimbursementDetailView(
        request.getId(),
        request.getProtocol(),
        request.getExpenseTypeCode(),
        names.getOrDefault(request.getBeneficiaryId(), "Beneficiario"),
        request.getStatus(),
        request.getCareDate(),
        request.getCreatedAt(),
        request.getExpectedPaymentDate(),
        request.getAmount(),
        request.getAmountReimbursed(),
        glosa.signum() > 0
            ? new ReimbursementDetailView.GlosaView(glosa, request.getGlosaReason())
            : null,
        request.getDenialReason(),
        request.getStatus() == ReimbursementStatus.PENDENTE_DOCUMENTACAO
            ? new ReimbursementDetailView.PendencyView(
                request.getPendencyDescription(), request.getPendencyDeadlineAt())
            : null,
        new ReimbursementDetailView.BankView(
            request.getBankCode(),
            request.getBankAgency(),
            request.maskedBankAccount(),
            request.getBankAccountType()),
        new ReimbursementDetailView.ProviderView(
            request.getProviderName(),
            request.getProviderCouncilCode(),
            request.getProviderCouncilNumber(),
            request.getProviderCouncilUf(),
            request.getProviderSpecialty()),
        request.getDocuments().stream()
            .map(
                document ->
                    new ReimbursementDetailView.DocumentView(
                        document.getCategory(),
                        document.getFileName(),
                        document.getFileSize(),
                        document.getUploadedAt()))
            .toList(),
        request.getTimelineEvents().stream()
            .map(
                event ->
                    new ReimbursementDetailView.TimelineView(
                        event.getOccurredAt(), event.getStatus(), event.getDescription()))
            .toList(),
        REGULATORY_NOTE);
  }

  private ReimbursementStatusChanged statusEvent(
      ReimbursementRequest request, String reason, Instant occurredAt) {
    return new ReimbursementStatusChanged(
        request.getId(),
        request.getBeneficiaryId(),
        request.getProtocol(),
        request.getStatus(),
        request.getAmountReimbursed(),
        request.getGlosaAmount(),
        reason,
        request.maskedBankAccount(),
        occurredAt);
  }

  private LocalDate expectedDate(String expenseType) {
    return BusinessDays.plus(
        LocalDate.now(clock),
        ExpenseTypeCodes.CONSULTA.equals(expenseType)
            ? CONSULTA_BUSINESS_DAYS
            : DEFAULT_BUSINESS_DAYS);
  }

  private static BigDecimal cap(BigDecimal amount, ReimbursementTableEntry tableEntry) {
    return amount.min(tableEntry.getAmount()).multiply(tableEntry.getPlanMultiple());
  }

  private static BigDecimal money(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
  }

  private void validateIdempotencyKey(String key) {
    if (key == null || key.isBlank() || key.length() > 100) {
      throw new ReimbursementIdempotencyKeyInvalidException();
    }
  }

  private static ReimbursementSubmissionResult submissionResultOf(ReimbursementRequest request) {
    return new ReimbursementSubmissionResult(
        request.getProtocol(), request.getStatus(), request.getExpectedPaymentDate());
  }

  private static boolean within(Instant value, LocalDate from, LocalDate to, ZoneId zone) {
    if (value == null) {
      return false;
    }
    LocalDate date = LocalDate.ofInstant(value, zone);
    return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
  }

  private static String normalizeCode(String value) {
    return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
  }

  private static String onlyDigits(String value) {
    return value == null ? "" : value.replaceAll("\\D", "");
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String safeFileName(String value) {
    if (value == null || value.isBlank()) {
      return "documento";
    }
    return value.length() <= 200 ? value : value.substring(0, 200);
  }

  private static String mask(UUID beneficiaryId) {
    String id = beneficiaryId.toString();
    return "***" + id.substring(id.length() - 4);
  }
}
