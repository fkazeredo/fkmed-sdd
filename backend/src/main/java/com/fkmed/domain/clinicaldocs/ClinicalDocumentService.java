package com.fkmed.domain.clinicaldocs;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service of the clinical-documents module's read-only API (SPEC-0011): the Minha Saúde
 * list (BR2), type-specific detail (BR6) and PDF download (BR7). Family scope reuses {@link
 * BeneficiaryAccess#accessibleFor} (SPEC-0003) exactly like {@code AppointmentService#list}; a
 * titular's access to a dependent's documents is audited (BR9) once per call — on the list only
 * when filtered to one specific dependent (AC3), and on the detail/PDF read of a dependent's
 * document — never per aggregated list item.
 */
@Service
@RequiredArgsConstructor
public class ClinicalDocumentService {

  private final ClinicalDocumentRepository documents;
  private final BeneficiaryAccess beneficiaryAccess;
  private final IdentityAccounts identityAccounts;
  private final AuditRecorder auditRecorder;
  private final Clock clock;

  /**
   * The Minha Saúde list across the caller's accessible beneficiaries (default "todos"), optionally
   * narrowed to one of them and to a category, within {@code period} (BR2/BR4/BR5/BR9),
   * most-recent-first. An unresolvable card or a filter outside the caller's scope yields an empty
   * list rather than an error — the list is a scoped aggregate, not a single-entity lookup.
   *
   * <p>Not {@code readOnly}: filtering to one specific dependent conditionally writes a BR9 audit
   * entry in the same call (a {@code readOnly} transaction would let Hibernate silently skip the
   * flush, dropping the audit write without any error — regression coverage in {@code
   * ClinicalDocumentApiIT}).
   */
  @Transactional
  public ClinicalDocumentListResponse list(
      String callerCard,
      String authorEmail,
      ClinicalDocumentType category,
      UUID beneficiaryFilter,
      DocumentPeriod period,
      AuditContext auditContext) {
    List<AccessibleBeneficiary> accessible = beneficiaryAccess.accessibleFor(callerCard);
    Map<UUID, String> namesById = toNameMap(accessible);
    if (namesById.isEmpty()
        || (beneficiaryFilter != null && !namesById.containsKey(beneficiaryFilter))) {
      return new ClinicalDocumentListResponse(List.of());
    }

    UUID selfId = accessible.get(0).beneficiaryId();
    if (beneficiaryFilter != null && !beneficiaryFilter.equals(selfId)) {
      auditDependentAccess(authorEmail, beneficiaryFilter, auditContext);
    }

    Collection<UUID> scopedIds =
        beneficiaryFilter != null ? Set.of(beneficiaryFilter) : namesById.keySet();
    ZoneId zone = clock.getZone();
    Instant from = period.from().atStartOfDay(zone).toInstant();
    Instant toExclusive = period.to().plusDays(1).atStartOfDay(zone).toInstant();
    List<ClinicalDocument> found =
        documents
            .findByBeneficiaryIdInAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtDesc(
                scopedIds, from, toExclusive);

    LocalDate today = LocalDate.now(clock);
    List<ClinicalDocumentListResponse.Item> items =
        found.stream()
            .filter(document -> category == null || document.getType() == category)
            .map(document -> toItem(document, namesById, zone, today))
            .toList();
    return new ClinicalDocumentListResponse(items);
  }

  /**
   * The type-specific detail of {@code documentId} within the caller's family scope (BR6/BR9).
   *
   * @throws ClinicalDocumentNotFoundException when unknown or out of scope (existence never
   *     revealed).
   */
  @Transactional
  public ClinicalDocumentDetail detail(
      String callerCard, String authorEmail, UUID documentId, AuditContext auditContext) {
    List<AccessibleBeneficiary> accessible = beneficiaryAccess.accessibleFor(callerCard);
    Map<UUID, String> namesById = toNameMap(accessible);
    ClinicalDocument document =
        documents
            .findById(documentId)
            .filter(candidate -> namesById.containsKey(candidate.getBeneficiaryId()))
            .orElseThrow(ClinicalDocumentNotFoundException::new);

    UUID selfId = accessible.get(0).beneficiaryId();
    if (!document.getBeneficiaryId().equals(selfId)) {
      auditDependentAccess(authorEmail, document.getBeneficiaryId(), auditContext);
    }

    ZoneId zone = clock.getZone();
    LocalDate today = LocalDate.now(clock);
    return toDetail(document, namesById.get(document.getBeneficiaryId()), zone, today);
  }

  /**
   * The same document rendered as a downloadable PDF (BR7). Reuses {@link #detail} so the scope
   * decision and the BR9 audit happen exactly once, the same way for both endpoints (mirrors {@code
   * CardService#cardPdfFor}). Not {@code readOnly} for the same reason as {@link #detail}: self
   * -invocation bypasses the Spring proxy, so this method's own transaction attribute — not {@link
   * #detail}'s — governs the audit write when this entry point is called externally.
   */
  @Transactional
  public byte[] pdfFor(
      String callerCard, String authorEmail, UUID documentId, AuditContext auditContext) {
    ClinicalDocumentDetail detail = detail(callerCard, authorEmail, documentId, auditContext);
    return ClinicalDocumentPdfRenderer.render(detail);
  }

  private void auditDependentAccess(
      String authorEmail, UUID targetBeneficiaryId, AuditContext auditContext) {
    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.DEPENDENT_CLINICAL_DOCUMENT_VIEWED,
            authorAccountIdFor(authorEmail),
            targetBeneficiaryId,
            Map.of(),
            auditContext));
  }

  private UUID authorAccountIdFor(String email) {
    return identityAccounts.findByEmail(email).map(AccountCredentials::accountId).orElse(null);
  }

  private static Map<UUID, String> toNameMap(List<AccessibleBeneficiary> accessible) {
    return accessible.stream()
        .collect(
            Collectors.toMap(
                AccessibleBeneficiary::beneficiaryId, AccessibleBeneficiary::firstName));
  }

  private static ClinicalDocumentListResponse.Item toItem(
      ClinicalDocument document, Map<UUID, String> names, ZoneId zone, LocalDate today) {
    return new ClinicalDocumentListResponse.Item(
        document.getId(),
        document.getType(),
        document.getProfessionalName(),
        document.getCrm(),
        LocalDate.ofInstant(document.getIssuedAt(), zone),
        names.get(document.getBeneficiaryId()),
        document.getValidUntil(),
        document.expired(today));
  }

  private static ClinicalDocumentDetail toDetail(
      ClinicalDocument document, String beneficiaryName, ZoneId zone, LocalDate today) {
    return new ClinicalDocumentDetail(
        document.getId(),
        document.getType(),
        LocalDate.ofInstant(document.getIssuedAt(), zone),
        document.getProfessionalName(),
        document.getCrm(),
        beneficiaryName,
        document.getValidUntil(),
        document.expired(today),
        document.getClinicalIndication(),
        document.getExamItems().stream()
            .map(
                item ->
                    new ClinicalDocumentDetail.ExamItemView(item.getExamName(), item.getTussCode()))
            .toList(),
        document.getTargetSpecialtyCode(),
        document.getReferralReason(),
        document.getPrescriptionItems().stream()
            .map(
                item ->
                    new ClinicalDocumentDetail.PrescriptionItemView(
                        item.getMedication(), item.getPosology(), item.getGuidance()))
            .toList(),
        document.getSickNotePeriodStart(),
        document.getSickNotePeriodEnd(),
        document.getCid(),
        document.getSickNoteNotes());
  }
}
