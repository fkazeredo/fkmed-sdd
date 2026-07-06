package com.fkmed.domain.clinicaldocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.AccountStatus;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.BeneficiaryRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SPEC-0011: family-scope resolution (BR2/BR9), the BR9 dependent-access audit — fired on the list
 * only when filtered to one specific dependent (AC3), and on detail/PDF reads of a dependent's
 * document — and the BR6/BR9 404 (existence never revealed). Domain/unit layer, mocking the
 * plan/identity/audit facades and the repository (mirrors {@code CardServiceTest}).
 */
@ExtendWith(MockitoExtension.class)
class ClinicalDocumentServiceTest {

  private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZONE);
  private static final String CALLER_CARD = "001234567";
  private static final String CALLER_EMAIL = "maria@fkmed.local";
  private static final UUID MARIA_ID = UUID.randomUUID();
  private static final UUID PEDRO_ID = UUID.randomUUID();
  private static final UUID MARIA_ACCOUNT_ID = UUID.randomUUID();
  private static final AuditContext NO_CONTEXT = AuditContext.none();
  private static final DocumentOrigin ORIGIN = DocumentOrigin.ofSession(UUID.randomUUID());

  @Mock private ClinicalDocumentRepository documents;
  @Mock private BeneficiaryAccess beneficiaryAccess;
  @Mock private IdentityAccounts identityAccounts;
  @Mock private AuditRecorder auditRecorder;

  private ClinicalDocumentService service;

  @BeforeEach
  void setUp() {
    service =
        new ClinicalDocumentService(
            documents, beneficiaryAccess, identityAccounts, auditRecorder, CLOCK);
  }

  @Test
  void list_filteredToSelf_recordsNoAudit() {
    givenFamilyScope();
    when(documents
            .findByBeneficiaryIdInAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtDesc(
                anyCollection(), any(), any()))
        .thenReturn(List.of(prescription(MARIA_ID)));

    ClinicalDocumentListResponse response =
        service.list(
            CALLER_CARD,
            CALLER_EMAIL,
            null,
            MARIA_ID,
            new DocumentPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
            NO_CONTEXT);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).beneficiary()).isEqualTo("MARIA");
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void list_filteredToDependent_recordsOneAuditEntry() {
    givenFamilyScope();
    when(identityAccounts.findByEmail(CALLER_EMAIL))
        .thenReturn(Optional.of(credentialsFor(MARIA_ACCOUNT_ID)));
    when(documents
            .findByBeneficiaryIdInAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtDesc(
                anyCollection(), any(), any()))
        .thenReturn(List.of(prescription(PEDRO_ID)));

    ClinicalDocumentListResponse response =
        service.list(
            CALLER_CARD,
            CALLER_EMAIL,
            null,
            PEDRO_ID,
            new DocumentPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
            NO_CONTEXT);

    assertThat(response.items()).hasSize(1);
    ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder, times(1)).record(captor.capture());
    assertThat(captor.getValue().eventType())
        .isEqualTo(AuditEventTypes.DEPENDENT_CLINICAL_DOCUMENT_VIEWED);
    assertThat(captor.getValue().authorAccountId()).isEqualTo(MARIA_ACCOUNT_ID);
    assertThat(captor.getValue().targetBeneficiaryId()).isEqualTo(PEDRO_ID);
  }

  @Test
  void list_filteredToAll_aggregatesAcrossFamily_recordsNoAudit() {
    givenFamilyScope();
    when(documents
            .findByBeneficiaryIdInAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtDesc(
                anyCollection(), any(), any()))
        .thenReturn(List.of(prescription(MARIA_ID), prescription(PEDRO_ID)));

    ClinicalDocumentListResponse response =
        service.list(
            CALLER_CARD,
            CALLER_EMAIL,
            null,
            null,
            new DocumentPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
            NO_CONTEXT);

    assertThat(response.items()).hasSize(2);
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void list_filteredToOutOfScopeBeneficiary_returnsEmpty_withoutQuerying() {
    givenFamilyScope();

    ClinicalDocumentListResponse response =
        service.list(
            CALLER_CARD,
            CALLER_EMAIL,
            null,
            UUID.randomUUID(),
            new DocumentPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
            NO_CONTEXT);

    assertThat(response.items()).isEmpty();
    verify(documents, never())
        .findByBeneficiaryIdInAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtDesc(
            any(), any(), any());
  }

  @Test
  void list_categoryFilter_keepsOnlyMatchingType() {
    givenFamilyScope();
    when(documents
            .findByBeneficiaryIdInAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtDesc(
                anyCollection(), any(), any()))
        .thenReturn(List.of(prescription(MARIA_ID), examOrder(MARIA_ID)));

    ClinicalDocumentListResponse response =
        service.list(
            CALLER_CARD,
            CALLER_EMAIL,
            ClinicalDocumentType.EXAM_ORDER,
            null,
            new DocumentPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
            NO_CONTEXT);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).type()).isEqualTo(ClinicalDocumentType.EXAM_ORDER);
  }

  @Test
  void detail_selfView_recordsNoAudit() {
    givenFamilyScope();
    ClinicalDocument document = prescription(MARIA_ID);
    when(documents.findById(document.getId())).thenReturn(Optional.of(document));

    ClinicalDocumentDetail detail =
        service.detail(CALLER_CARD, CALLER_EMAIL, document.getId(), NO_CONTEXT);

    assertThat(detail.beneficiary()).isEqualTo("MARIA");
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void detail_dependentView_recordsOneAuditEntry() {
    givenFamilyScope();
    when(identityAccounts.findByEmail(CALLER_EMAIL))
        .thenReturn(Optional.of(credentialsFor(MARIA_ACCOUNT_ID)));
    ClinicalDocument document = prescription(PEDRO_ID);
    when(documents.findById(document.getId())).thenReturn(Optional.of(document));

    service.detail(CALLER_CARD, CALLER_EMAIL, document.getId(), NO_CONTEXT);

    ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder, times(1)).record(captor.capture());
    assertThat(captor.getValue().targetBeneficiaryId()).isEqualTo(PEDRO_ID);
  }

  @Test
  void detail_unknownDocument_throwsNotFound() {
    givenFamilyScope();
    UUID unknownId = UUID.randomUUID();
    when(documents.findById(unknownId)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ClinicalDocumentNotFoundException.class)
        .isThrownBy(() -> service.detail(CALLER_CARD, CALLER_EMAIL, unknownId, NO_CONTEXT))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo("document.not-found"));
  }

  @Test
  void detail_outOfScopeDocument_throwsNotFound_withoutRevealingExistence() {
    // Only MARIA/PEDRO accessible; a third-family document must 404 exactly like an unknown id.
    givenFamilyScope();
    ClinicalDocument thirdPartyDocument = prescription(UUID.randomUUID());
    when(documents.findById(thirdPartyDocument.getId()))
        .thenReturn(Optional.of(thirdPartyDocument));

    assertThatExceptionOfType(ClinicalDocumentNotFoundException.class)
        .isThrownBy(
            () ->
                service.detail(CALLER_CARD, CALLER_EMAIL, thirdPartyDocument.getId(), NO_CONTEXT));
  }

  @Test
  void pdfFor_rendersAPdfDocument_reusingDetailsAuditBehavior() {
    givenFamilyScope();
    when(identityAccounts.findByEmail(CALLER_EMAIL))
        .thenReturn(Optional.of(credentialsFor(MARIA_ACCOUNT_ID)));
    ClinicalDocument document = prescription(PEDRO_ID);
    when(documents.findById(document.getId())).thenReturn(Optional.of(document));

    byte[] pdf = service.pdfFor(CALLER_CARD, CALLER_EMAIL, document.getId(), NO_CONTEXT);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("%PDF-");
    verify(auditRecorder, times(1)).record(any());
  }

  private void givenFamilyScope() {
    when(beneficiaryAccess.accessibleFor(CALLER_CARD))
        .thenReturn(
            List.of(
                new AccessibleBeneficiary(MARIA_ID, "MARIA", BeneficiaryRole.TITULAR),
                new AccessibleBeneficiary(PEDRO_ID, "PEDRO", BeneficiaryRole.DEPENDENT)));
  }

  private static ClinicalDocument prescription(UUID beneficiaryId) {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.prescription(
            beneficiaryId,
            "Dra. Camila Andrade",
            "CRM 55214 RJ",
            ORIGIN,
            List.of(new PrescriptionItemInput("Amoxicilina 500mg", "1cp 8/8h", null)));
    return ClinicalDocument.issue(command, CLOCK.instant(), LocalDate.now(CLOCK));
  }

  private static ClinicalDocument examOrder(UUID beneficiaryId) {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.examOrder(
            beneficiaryId,
            "Dra. Camila Andrade",
            "CRM 55214 RJ",
            ORIGIN,
            "Investigação de fadiga",
            List.of(new ExamItemInput("Hemograma Completo", "40304361")));
    return ClinicalDocument.issue(command, CLOCK.instant(), LocalDate.now(CLOCK));
  }

  private static AccountCredentials credentialsFor(UUID accountId) {
    return new AccountCredentials(
        accountId, CALLER_EMAIL, "hash", AccountStatus.ACTIVE, UUID.randomUUID(), false);
  }
}
