package com.fkmed.application.sim;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.clinicaldocs.ClinicalDocuments;
import com.fkmed.domain.clinicaldocs.DocumentOrigin;
import com.fkmed.domain.clinicaldocs.ExamItemInput;
import com.fkmed.domain.clinicaldocs.IssueClinicalDocumentCommand;
import com.fkmed.domain.clinicaldocs.PrescriptionItemInput;
import com.fkmed.domain.finance.Copays;
import com.fkmed.domain.finance.InvoiceIssuedResult;
import com.fkmed.domain.finance.Invoices;
import com.fkmed.domain.finance.IssueInvoiceCommand;
import com.fkmed.domain.guides.GuideItemInput;
import com.fkmed.domain.guides.GuideItemStatus;
import com.fkmed.domain.guides.GuideNotFoundException;
import com.fkmed.domain.guides.GuideService;
import com.fkmed.domain.guides.GuideTransitionResult;
import com.fkmed.domain.guides.GuideType;
import com.fkmed.domain.network.NetworkSpecialties;
import com.fkmed.domain.network.SpecialtyOption;
import com.fkmed.domain.telemedicine.TeleClosureSummary;
import com.fkmed.domain.telemedicine.TeleService;
import com.fkmed.domain.telemedicine.TeleSessionNotFoundException;
import com.fkmed.domain.telemedicine.TeleSessionView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application-layer adapter of the operator-simulation tele slice (SPEC-0018 BR5,
 * ADR-0017/DL-0021): it drives the {@code domain.telemedicine} and {@code domain.clinicaldocs}
 * facades — NOT a domain module of its own (Rule Zero). Every action produces the same domain
 * events/notifications a real back office would (BR3), so consuming modules cannot tell the
 * difference, and is audited with the operator as author. Owning-module state-machine guards (an
 * {@link IllegalStateException} from the tele transition) and not-found are translated to the sim's
 * stable {@code 409}/{@code 404} contract (BR4). Closure issues the documents ATOMICALLY with the
 * close in one transaction (SPEC-0010 BR10).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimService {

  private final TeleService tele;
  private final ClinicalDocuments clinicalDocuments;
  private final GuideService guides;
  private final Invoices invoices;
  private final Copays copays;
  private final NetworkSpecialties specialties;
  private final AuditRecorder auditRecorder;

  /** Starts attending the next queued session (BR5): reaches its turn and reports its new state. */
  @Transactional
  public SimTeleSessionResult startNextTele(
      String professionalName, String crm, UUID operatorAccountId, AuditContext auditContext) {
    UUID sessionId;
    try {
      sessionId = tele.reachNextTurn(professionalName, crm);
    } catch (TeleSessionNotFoundException notFound) {
      throw new SimTargetNotFoundException();
    } catch (IllegalStateException invalid) {
      throw new SimInvalidTransitionException();
    }
    audit(operatorAccountId, null, auditContext, "tele.start-next", sessionId);
    log.info("sim: operator started attending session {}", sessionId);
    return new SimTeleSessionResult(sessionId, stateOf(sessionId), List.of());
  }

  /**
   * Closes a session with the professional's summary and issues its clinical documents atomically
   * (BR5, SPEC-0010 BR9/BR10): the close and every issuance share this transaction, so either all
   * commit or none does; the documents are bound to the attended beneficiary and the session
   * origin.
   */
  @Transactional
  public SimTeleSessionResult closeTele(
      UUID sessionId,
      String professionalName,
      String crm,
      String guidance,
      List<SimDocumentSpec> documents,
      UUID operatorAccountId,
      AuditContext auditContext) {
    UUID beneficiaryId;
    try {
      beneficiaryId =
          tele.close(sessionId, new TeleClosureSummary(professionalName, crm, guidance));
    } catch (TeleSessionNotFoundException notFound) {
      throw new SimTargetNotFoundException();
    } catch (IllegalStateException invalid) {
      throw new SimInvalidTransitionException();
    }

    Map<String, String> specialtyNames = specialtyNames();
    List<UUID> issued = new ArrayList<>();
    for (SimDocumentSpec document : documents) {
      issued.add(
          clinicalDocuments.issue(
              commandOf(
                  document,
                  beneficiaryId,
                  professionalName,
                  crm,
                  DocumentOrigin.ofSession(sessionId),
                  specialtyNames)));
    }
    audit(operatorAccountId, beneficiaryId, auditContext, "tele.close", sessionId);
    log.info("sim: operator closed session {} issuing {} document(s)", sessionId, issued.size());
    return new SimTeleSessionResult(sessionId, "ENCERRADA", issued);
  }

  /** Issues an operator-authored document for a beneficiary (BR5), origin = operator. */
  @Transactional
  public UUID issueDocument(
      UUID beneficiaryId,
      String professionalName,
      String crm,
      SimDocumentSpec spec,
      UUID operatorAccountId,
      AuditContext auditContext) {
    UUID documentId =
        clinicalDocuments.issue(
            commandOf(
                spec,
                beneficiaryId,
                professionalName,
                crm,
                DocumentOrigin.ofOperator(operatorAccountId),
                specialtyNames()));
    audit(operatorAccountId, beneficiaryId, auditContext, "document.issue", documentId);
    log.info("sim: operator issued document {} for a beneficiary", documentId);
    return documentId;
  }

  /** Opens a new guide as {@code EM_ANALISE} (SPEC-0018 BR5, SPEC-0012). */
  @Transactional
  public SimGuideResult createGuide(
      UUID beneficiaryId,
      GuideType type,
      String requestingProvider,
      List<SimCreateGuideRequest.GuideItemRequest> items,
      UUID operatorAccountId,
      AuditContext auditContext) {
    GuideTransitionResult guide =
        guides.createGuide(
            type,
            beneficiaryId,
            requestingProvider,
            items.stream()
                .map(
                    item ->
                        new GuideItemInput(item.tussCode(), item.description(), item.quantity()))
                .toList());
    audit(operatorAccountId, beneficiaryId, auditContext, "guide.create", guide.id());
    return resultOf(guide);
  }

  /**
   * Authorizes every item of a guide (SPEC-0018 BR5).
   *
   * @throws SimTargetNotFoundException when the guide is unknown.
   * @throws SimInvalidTransitionException when the guide is not {@code EM_ANALISE} (BR4).
   */
  @Transactional
  public SimGuideResult authorizeGuide(
      UUID guideId,
      String password,
      LocalDate validUntil,
      UUID operatorAccountId,
      AuditContext auditContext) {
    GuideTransitionResult guide =
        guardGuideTransition(() -> guides.authorize(guideId, password, validUntil));
    audit(operatorAccountId, guide.beneficiaryId(), auditContext, "guide.authorize", guideId);
    return resultOf(guide);
  }

  /**
   * Applies a per-item authorization decision (SPEC-0018 BR5); the overall status derives from the
   * items (SPEC-0012 BR6).
   *
   * @throws SimTargetNotFoundException when the guide is unknown.
   * @throws SimInvalidTransitionException when the guide is not {@code EM_ANALISE} (BR4).
   */
  @Transactional
  public SimGuideResult partiallyAuthorizeGuide(
      UUID guideId,
      String password,
      LocalDate validUntil,
      Map<String, GuideItemStatus> itemStatuses,
      UUID operatorAccountId,
      AuditContext auditContext) {
    GuideTransitionResult guide =
        guardGuideTransition(
            () -> guides.partiallyAuthorize(guideId, password, validUntil, itemStatuses));
    audit(
        operatorAccountId,
        guide.beneficiaryId(),
        auditContext,
        "guide.partially-authorize",
        guideId);
    return resultOf(guide);
  }

  /**
   * Denies every item of a guide (SPEC-0018 BR5).
   *
   * @throws SimTargetNotFoundException when the guide is unknown.
   * @throws SimInvalidTransitionException when the guide is not {@code EM_ANALISE} (BR4).
   */
  @Transactional
  public SimGuideResult denyGuide(
      UUID guideId, String reason, UUID operatorAccountId, AuditContext auditContext) {
    GuideTransitionResult guide = guardGuideTransition(() -> guides.deny(guideId, reason));
    audit(operatorAccountId, guide.beneficiaryId(), auditContext, "guide.deny", guideId);
    return resultOf(guide);
  }

  /**
   * Cancels a guide (SPEC-0018 BR5).
   *
   * @throws SimTargetNotFoundException when the guide is unknown.
   * @throws SimInvalidTransitionException when cancellation is not allowed from the current status
   *     (BR4).
   */
  @Transactional
  public SimGuideResult cancelGuide(
      UUID guideId, UUID operatorAccountId, AuditContext auditContext) {
    GuideTransitionResult guide = guardGuideTransition(() -> guides.cancel(guideId));
    audit(operatorAccountId, guide.beneficiaryId(), auditContext, "guide.cancel", guideId);
    return resultOf(guide);
  }

  /**
   * Marks a guide executed (SPEC-0018 BR5).
   *
   * @throws SimTargetNotFoundException when the guide is unknown.
   * @throws SimInvalidTransitionException when the guide is not authorized (fully or partially,
   *     BR4).
   */
  @Transactional
  public SimGuideResult markGuideExecuted(
      UUID guideId, UUID operatorAccountId, AuditContext auditContext) {
    GuideTransitionResult guide = guardGuideTransition(() -> guides.markExecuted(guideId));
    audit(operatorAccountId, guide.beneficiaryId(), auditContext, "guide.mark-executed", guideId);
    return resultOf(guide);
  }

  /**
   * Issues a new OPEN invoice for a contract titular (SPEC-0013 §Operator-sim), publishing {@code
   * InvoiceIssued} (→ SPEC-0004 notification). The digitable line is normalized to 47 digits by the
   * finance facade.
   */
  @Transactional
  public SimInvoiceResult issueInvoice(
      UUID titularBeneficiaryId,
      LocalDate competencia,
      LocalDate dueDate,
      BigDecimal amount,
      String digitableLine,
      String pixCode,
      UUID operatorAccountId,
      AuditContext auditContext) {
    InvoiceIssuedResult result =
        invoices.issue(
            new IssueInvoiceCommand(
                titularBeneficiaryId, competencia, dueDate, amount, digitableLine, pixCode));
    audit(
        operatorAccountId,
        titularBeneficiaryId,
        auditContext,
        "finance.issue-invoice",
        result.id());
    log.info("sim: operator issued invoice {} for a titular", result.id());
    return new SimInvoiceResult(result.id(), result.competencia(), "OPEN");
  }

  /**
   * Records the payment of an invoice idempotently (SPEC-0018 BR6): a repeat on an already-paid
   * invoice does not double-pay nor duplicate events.
   *
   * @throws SimTargetNotFoundException when no invoice with {@code invoiceId} exists.
   */
  @Transactional
  public void payInvoice(UUID invoiceId, UUID operatorAccountId, AuditContext auditContext) {
    if (!invoices.pay(invoiceId)) {
      throw new SimTargetNotFoundException();
    }
    audit(operatorAccountId, null, auditContext, "finance.pay-invoice", invoiceId);
    log.info("sim: operator paid invoice {}", invoiceId);
  }

  /** Records a copay charge for a family member's usage (SPEC-0013 §Operator-sim). */
  @Transactional
  public SimCopayResult recordCopay(
      LocalDate entryDate,
      String procedure,
      String provider,
      UUID beneficiaryId,
      BigDecimal amount,
      UUID operatorAccountId,
      AuditContext auditContext) {
    UUID id = copays.record(entryDate, procedure, provider, beneficiaryId, amount);
    audit(operatorAccountId, beneficiaryId, auditContext, "finance.record-copay", id);
    return new SimCopayResult(id);
  }

  /**
   * Translates the owning module's not-found/invalid-transition signals to the sim's stable
   * contract (BR4): {@link GuideNotFoundException} to {@code 404}, an {@link IllegalStateException}
   * (the guide state machine's guard) to {@code 409}.
   */
  private GuideTransitionResult guardGuideTransition(Supplier<GuideTransitionResult> transition) {
    try {
      return transition.get();
    } catch (GuideNotFoundException notFound) {
      throw new SimTargetNotFoundException();
    } catch (IllegalStateException invalid) {
      throw new SimInvalidTransitionException();
    }
  }

  private static SimGuideResult resultOf(GuideTransitionResult guide) {
    return new SimGuideResult(guide.id(), guide.number(), guide.status());
  }

  private String stateOf(UUID sessionId) {
    return tele.viewOf(sessionId).map(TeleSessionView::state).orElse("EM_ATENDIMENTO");
  }

  private IssueClinicalDocumentCommand commandOf(
      SimDocumentSpec spec,
      UUID beneficiaryId,
      String professionalName,
      String crm,
      DocumentOrigin origin,
      Map<String, String> specialtyNames) {
    return switch (spec.type()) {
      case EXAM_ORDER ->
          IssueClinicalDocumentCommand.examOrder(
              beneficiaryId,
              professionalName,
              crm,
              origin,
              spec.clinicalIndication(),
              examItems(spec));
      case REFERRAL ->
          IssueClinicalDocumentCommand.referral(
              beneficiaryId,
              professionalName,
              crm,
              origin,
              spec.specialtyCode(),
              specialtyNames.getOrDefault(spec.specialtyCode(), spec.specialtyCode()),
              spec.reason());
      case PRESCRIPTION ->
          IssueClinicalDocumentCommand.prescription(
              beneficiaryId, professionalName, crm, origin, medications(spec));
      case SICK_NOTE ->
          IssueClinicalDocumentCommand.sickNote(
              beneficiaryId,
              professionalName,
              crm,
              origin,
              spec.periodStart(),
              spec.periodEnd(),
              spec.cid(),
              spec.notes());
    };
  }

  private static List<ExamItemInput> examItems(SimDocumentSpec spec) {
    if (spec.exams() == null) {
      return List.of();
    }
    return spec.exams().stream().map(item -> new ExamItemInput(item.name(), item.tuss())).toList();
  }

  private static List<PrescriptionItemInput> medications(SimDocumentSpec spec) {
    if (spec.medications() == null) {
      return List.of();
    }
    return spec.medications().stream()
        .map(item -> new PrescriptionItemInput(item.medication(), item.posology(), item.guidance()))
        .toList();
  }

  private Map<String, String> specialtyNames() {
    return specialties.all().stream()
        .collect(Collectors.toMap(SpecialtyOption::code, SpecialtyOption::name, (a, b) -> a));
  }

  private void audit(
      UUID operatorAccountId,
      UUID targetBeneficiaryId,
      AuditContext auditContext,
      String action,
      UUID targetId) {
    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.OPERATOR_SIM_ACTION,
            operatorAccountId,
            targetBeneficiaryId,
            Map.of("action", action, "target", String.valueOf(targetId)),
            auditContext));
  }
}
