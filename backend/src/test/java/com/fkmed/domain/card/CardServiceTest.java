package com.fkmed.domain.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.CardDetails;
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
 * SPEC-0007: card resolution, the BR10 inactive/unavailable state and the BR7 dependent-view audit
 * (domain/unit layer, mocking the plan/identity/audit facades).
 */
@ExtendWith(MockitoExtension.class)
class CardServiceTest {

  private static final String MARIA_CARD = "001234567";
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final UUID MARIA_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID PEDRO_ID = UUID.randomUUID();
  private static final AuditContext NO_CONTEXT = AuditContext.none();

  @Mock private BeneficiaryAccess beneficiaryAccess;
  @Mock private IdentityAccounts identityAccounts;
  @Mock private AuditRecorder auditRecorder;

  private CardService service;

  @BeforeEach
  void setUp() {
    service = new CardService(beneficiaryAccess, identityAccounts, auditRecorder);
  }

  @Test
  void cardFor_selfView_returnsCnsInFull_andRecordsNoAudit() {
    UUID maria = UUID.randomUUID();
    when(beneficiaryAccess.cardDetailsFor(MARIA_CARD, maria)).thenReturn(activeSelfView(maria));

    CardResponse response = service.cardFor(MARIA_CARD, MARIA_EMAIL, maria, NO_CONTEXT);

    assertThat(response.cns()).isEqualTo("700000000000001");
    assertThat(response.planCategory()).isEqualTo("Coletivo por Adesão");
    verify(auditRecorder, never()).record(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void cardFor_dependentView_recordsOneAuditEntry_withAuthorAndTargetIds() {
    when(beneficiaryAccess.cardDetailsFor(MARIA_CARD, PEDRO_ID)).thenReturn(activeDependentView());
    when(identityAccounts.findByEmail(MARIA_EMAIL))
        .thenReturn(Optional.of(credentialsFor(MARIA_ACCOUNT_ID)));

    CardResponse response = service.cardFor(MARIA_CARD, MARIA_EMAIL, PEDRO_ID, NO_CONTEXT);

    assertThat(response.fullName()).isEqualTo("PEDRO SOUZA LIMA");
    ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder, times(1)).record(captor.capture());
    AuditEntry entry = captor.getValue();
    assertThat(entry.eventType()).isEqualTo(AuditEventTypes.DEPENDENT_CARD_VIEWED);
    assertThat(entry.authorAccountId()).isEqualTo(MARIA_ACCOUNT_ID);
    assertThat(entry.targetBeneficiaryId()).isEqualTo(PEDRO_ID);
  }

  @Test
  void cardFor_dependentView_withNoMatchingAccount_recordsAuditWithNullAuthor() {
    when(beneficiaryAccess.cardDetailsFor(MARIA_CARD, PEDRO_ID)).thenReturn(activeDependentView());
    when(identityAccounts.findByEmail(MARIA_EMAIL)).thenReturn(Optional.empty());

    service.cardFor(MARIA_CARD, MARIA_EMAIL, PEDRO_ID, NO_CONTEXT);

    ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder).record(captor.capture());
    assertThat(captor.getValue().authorAccountId()).isNull();
  }

  @Test
  void cardFor_inactiveBeneficiary_throwsCardUnavailable_andRecordsNoAudit() {
    when(beneficiaryAccess.cardDetailsFor(MARIA_CARD, PEDRO_ID))
        .thenReturn(inactiveDependentView());

    assertThatExceptionOfType(CardUnavailableException.class)
        .isThrownBy(() -> service.cardFor(MARIA_CARD, MARIA_EMAIL, PEDRO_ID, NO_CONTEXT))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo("card.unavailable"));
    verify(auditRecorder, never()).record(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void cardPdfFor_rendersAPdfDocument_reusingCardForsAuditBehavior() {
    when(beneficiaryAccess.cardDetailsFor(MARIA_CARD, PEDRO_ID)).thenReturn(activeDependentView());
    when(identityAccounts.findByEmail(MARIA_EMAIL))
        .thenReturn(Optional.of(credentialsFor(MARIA_ACCOUNT_ID)));

    byte[] pdf = service.cardPdfFor(MARIA_CARD, MARIA_EMAIL, PEDRO_ID, NO_CONTEXT);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("%PDF-");
    verify(auditRecorder, times(1)).record(org.mockito.ArgumentMatchers.any());
  }

  private static CardDetails activeSelfView(UUID beneficiaryId) {
    return new CardDetails(
        "MARIA CLARA SOUZA LIMA",
        MARIA_CARD,
        "700000000000001",
        true,
        false,
        "PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP",
        "326305",
        "ESTADUAL",
        "Coletivo por Adesão",
        List.of("Urg/emerg Nacional Hr — Assistência"));
  }

  private static CardDetails activeDependentView() {
    return new CardDetails(
        "PEDRO SOUZA LIMA",
        "001234575",
        "700000000000002",
        true,
        true,
        "PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP",
        "326305",
        "ESTADUAL",
        "Coletivo por Adesão",
        List.of("Urg/emerg Nacional Hr — Assistência"));
  }

  private static CardDetails inactiveDependentView() {
    return new CardDetails(
        "PEDRO SOUZA LIMA",
        "001234575",
        "700000000000002",
        false,
        true,
        "PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP",
        "326305",
        "ESTADUAL",
        "Coletivo por Adesão",
        List.of("Urg/emerg Nacional Hr — Assistência"));
  }

  private static AccountCredentials credentialsFor(UUID accountId) {
    return new AccountCredentials(
        accountId, MARIA_EMAIL, "hash", AccountStatus.ACTIVE, UUID.randomUUID(), false);
  }
}
