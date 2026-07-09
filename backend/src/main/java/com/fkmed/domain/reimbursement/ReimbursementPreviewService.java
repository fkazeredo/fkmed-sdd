package com.fkmed.domain.reimbursement;

import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.ProtocolGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for non-binding reimbursement previews (SPEC-0017). */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReimbursementPreviewService {

  public static final String BASE = "tabela do plano vigente";
  public static final String DISCLAIMER =
      "A prévia é uma estimativa com base nas informações e documentos enviados e nas regras do seu plano. "
          + "Não representa autorização nem garantia de pagamento.";

  private static final String PROTOCOL_PREFIX = "PV";

  private final ReimbursementPreviewRepository previews;
  private final BeneficiaryAccess beneficiaryAccess;
  private final ProtocolGenerator protocolGenerator;
  private final ReimbursementService reimbursements;
  private final ApplicationEventPublisher events;
  private final MeterRegistry metrics;
  private final Clock clock;

  @Transactional
  public ReimbursementPreviewResult create(
      String callerCard,
      UUID authorAccountId,
      UUID beneficiaryId,
      String expenseTypeCode,
      List<UploadedDocument> documents) {
    reimbursements.requireEligible(callerCard);
    beneficiaryAccess.requireAccessible(callerCard, beneficiaryId);
    String expenseType = reimbursements.validateExpenseType(expenseTypeCode);
    ReimbursementTableEntry tableEntry = reimbursements.requireTableEntry(expenseType);
    Instant now = clock.instant();
    String protocol = protocolGenerator.next(PROTOCOL_PREFIX);
    ReimbursementPreview preview;
    if (ExpenseTypeCodes.CONSULTA.equals(expenseType)) {
      preview =
          ReimbursementPreview.immediate(
              protocol, beneficiaryId, expenseType, tableEntry.getAmount(), authorAccountId, now);
      events.publishEvent(
          new PreviewConcluded(preview.getId(), beneficiaryId, protocol, tableEntry.getAmount()));
      metrics.counter("reimbursement.preview.created", "mode", "immediate").increment();
    } else {
      List<StoredDocument> stored = reimbursements.validateAndStorePreviewDocuments(documents);
      preview =
          ReimbursementPreview.analyzed(
              protocol, beneficiaryId, expenseType, stored, authorAccountId, now);
      metrics.counter("reimbursement.preview.created", "mode", "analyzed").increment();
    }
    previews.save(preview);
    log.info("reimbursement preview {} created", protocol);
    return resultOf(preview, accessibleNames(callerCard));
  }

  public List<ReimbursementPreviewListItem> list(String callerCard, UUID beneficiaryId) {
    reimbursements.requireEligible(callerCard);
    Map<UUID, String> names = accessibleNames(callerCard);
    return previews
        .findByBeneficiaryIdInOrderByCreatedAtDesc(
            reimbursements.targetIds(callerCard, beneficiaryId))
        .stream()
        .map(preview -> listItemOf(preview, names))
        .toList();
  }

  public ReimbursementPreviewResult detail(String callerCard, UUID id) {
    reimbursements.requireEligible(callerCard);
    Map<UUID, String> names = accessibleNames(callerCard);
    ReimbursementPreview preview =
        previews
            .findByIdAndBeneficiaryIdIn(id, reimbursements.targetIds(callerCard, null))
            .orElseThrow(PreviewNotFoundException::new);
    return resultOf(preview, names);
  }

  @Transactional
  public ReimbursementPreviewResult conclude(UUID id, BigDecimal estimatedValue) {
    ReimbursementPreview preview = previews.findById(id).orElseThrow(PreviewNotFoundException::new);
    preview.conclude(estimatedValue, clock.instant());
    events.publishEvent(
        new PreviewConcluded(
            preview.getId(), preview.getBeneficiaryId(), preview.getProtocol(), estimatedValue));
    metrics.counter("reimbursement.preview.concluded").increment();
    return resultOf(preview, Map.of());
  }

  private ReimbursementPreviewResult resultOf(
      ReimbursementPreview preview, Map<UUID, String> names) {
    return new ReimbursementPreviewResult(
        preview.getId(),
        preview.getProtocol(),
        preview.getExpenseTypeCode(),
        names.getOrDefault(preview.getBeneficiaryId(), "Beneficiario"),
        preview.getSituation(),
        preview.getEstimatedValue(),
        preview.getCreatedAt(),
        preview.getConcludedAt(),
        BASE,
        DISCLAIMER);
  }

  private ReimbursementPreviewListItem listItemOf(
      ReimbursementPreview preview, Map<UUID, String> names) {
    return new ReimbursementPreviewListItem(
        preview.getId(),
        preview.getProtocol(),
        preview.getExpenseTypeCode(),
        names.getOrDefault(preview.getBeneficiaryId(), "Beneficiario"),
        preview.getCreatedAt(),
        preview.getSituation(),
        preview.getEstimatedValue());
  }

  private Map<UUID, String> accessibleNames(String callerCard) {
    return reimbursements.accessibleNames(callerCard);
  }
}
