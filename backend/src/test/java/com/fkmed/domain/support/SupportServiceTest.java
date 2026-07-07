package com.fkmed.domain.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.AccountStatus;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.BeneficiaryNotAccessibleException;
import com.fkmed.domain.plan.BeneficiaryRole;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * SPEC-0014 §Tests Required (domain/unit): FAQ search normalizes case/accents over title AND
 * content (BR5); the Libras hours window gates the registration outcome (BR4); the write scope-
 * checks the beneficiary and always audits (unlike {@code TokenService}'s dependent-only rule).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupportServiceTest {

  private static final UUID BENEFICIARY = UUID.fromString("b0000000-0000-4000-8000-000000000001");
  private static final String CARD = "001234567";
  private static final UUID AUTHOR_ACCOUNT =
      UUID.fromString("a0000000-0000-4000-8000-000000000001");

  @Mock private SupportChannelRepository channels;
  @Mock private FaqEntryRepository faqEntries;
  @Mock private SupportAntifraudRepository antifraudContent;
  @Mock private LibrasRequestRepository librasRequests;
  @Mock private BeneficiaryAccess beneficiaryAccess;
  @Mock private IdentityAccounts identityAccounts;
  @Mock private AuditRecorder auditRecorder;

  private SimpleMeterRegistry metrics;

  @BeforeEach
  void setUp() {
    metrics = new SimpleMeterRegistry();
    when(identityAccounts.findByEmail(any()))
        .thenReturn(
            Optional.of(
                new AccountCredentials(
                    AUTHOR_ACCOUNT,
                    "maria@fkmed.local",
                    "hash",
                    AccountStatus.ACTIVE,
                    BENEFICIARY,
                    false)));
    when(beneficiaryAccess.requireAccessible(CARD, BENEFICIARY))
        .thenReturn(new AccessibleBeneficiary(BENEFICIARY, "Maria", BeneficiaryRole.TITULAR));
  }

  @Test
  void faq_withAccentAndCaseInsensitiveQuery_matchesQuestionAndAnswer() {
    when(faqEntries.findByActiveTrueOrderByDisplayOrderAsc())
        .thenReturn(
            List.of(
                faqEntry(FaqCategoryCodes.BOLETOS, "Como validar um Boleto?", "Use o validador."),
                faqEntry(FaqCategoryCodes.REDE, "Como agendar consulta?", "Use a busca de rede.")));

    List<FaqEntryView> result = service().faq("boLEto", null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).question()).isEqualTo("Como validar um Boleto?");
  }

  @Test
  void faq_matchesInsideTheAnswerToo() {
    when(faqEntries.findByActiveTrueOrderByDisplayOrderAsc())
        .thenReturn(
            List.of(
                faqEntry(FaqCategoryCodes.REEMBOLSO, "Prazo de reembolso?", "Até 12 meses."),
                faqEntry(FaqCategoryCodes.REDE, "Como agendar?", "Use a busca de rede.")));

    List<FaqEntryView> result = service().faq("12 MESES", null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).category()).isEqualTo(FaqCategoryCodes.REEMBOLSO);
  }

  @Test
  void faq_withCategoryFilter_restrictsToCategory() {
    when(faqEntries.findByActiveTrueOrderByDisplayOrderAsc())
        .thenReturn(
            List.of(
                faqEntry(FaqCategoryCodes.BOLETOS, "Pergunta A", "Resposta A"),
                faqEntry(FaqCategoryCodes.REDE, "Pergunta B", "Resposta B")));

    List<FaqEntryView> result = service().faq(null, FaqCategoryCodes.REDE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).category()).isEqualTo(FaqCategoryCodes.REDE);
  }

  @Test
  void faq_withNoMatches_incrementsTheZeroResultCounter() {
    when(faqEntries.findByActiveTrueOrderByDisplayOrderAsc())
        .thenReturn(List.of(faqEntry(FaqCategoryCodes.REDE, "Pergunta", "Resposta")));

    List<FaqEntryView> result = service().faq("termo-inexistente", null);

    assertThat(result).isEmpty();
    assertThat(metrics.counter("support.faq.zero-results").count()).isEqualTo(1.0);
  }

  @Test
  void faq_withBlankQuery_listsEverything_withoutIncrementingTheCounter() {
    when(faqEntries.findByActiveTrueOrderByDisplayOrderAsc())
        .thenReturn(List.of(faqEntry(FaqCategoryCodes.REDE, "Pergunta", "Resposta")));

    List<FaqEntryView> result = service().faq("  ", null);

    assertThat(result).hasSize(1);
    assertThat(metrics.counter("support.faq.zero-results").count()).isZero();
  }

  @Test
  void requestLibras_withinHours_returnsVideocallShortly_andAudits() {
    Clock clock = Clock.fixed(Instant.parse("2026-07-06T13:00:00-03:00"), ZoneOffset.of("-03:00"));

    LibrasRequestResult result =
        service(clock).requestLibras(CARD, "maria@fkmed.local", BENEFICIARY, AuditContext.none());

    assertThat(result.nextStep()).isEqualTo("videocall-shortly");
    assertThat(result.hoursStart()).isNull();
    ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditRecorder).record(captor.capture());
    assertThat(captor.getValue().targetBeneficiaryId()).isEqualTo(BENEFICIARY);
    assertThat(captor.getValue().authorAccountId()).isEqualTo(AUTHOR_ACCOUNT);
  }

  @Test
  void requestLibras_outsideHours_returnsNextPeriodWithHours() {
    // A Sunday — outside the Mon-Fri window regardless of the time of day.
    Clock clock = Clock.fixed(Instant.parse("2026-07-05T13:00:00-03:00"), ZoneOffset.of("-03:00"));

    LibrasRequestResult result =
        service(clock).requestLibras(CARD, "maria@fkmed.local", BENEFICIARY, AuditContext.none());

    assertThat(result.nextStep()).isEqualTo("next-period");
    assertThat(result.hoursStart()).isNotNull();
    assertThat(result.hoursEnd()).isNotNull();
  }

  @Test
  void requestLibras_forAnOutOfScopeBeneficiary_throwsWithoutSavingOrAuditing() {
    UUID outOfScope = UUID.randomUUID();
    when(beneficiaryAccess.requireAccessible(CARD, outOfScope))
        .thenThrow(new BeneficiaryNotAccessibleException());

    assertThatExceptionOfType(BeneficiaryNotAccessibleException.class)
        .isThrownBy(
            () ->
                service()
                    .requestLibras(CARD, "maria@fkmed.local", outOfScope, AuditContext.none()));
    verify(librasRequests, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  private SupportService service() {
    return service(
        Clock.fixed(Instant.parse("2026-07-06T13:00:00-03:00"), ZoneOffset.of("-03:00")));
  }

  private SupportService service(Clock clock) {
    return new SupportService(
        channels,
        faqEntries,
        antifraudContent,
        librasRequests,
        beneficiaryAccess,
        identityAccounts,
        auditRecorder,
        metrics,
        clock);
  }

  private static FaqEntry faqEntry(String category, String question, String answer) {
    return FaqEntry.of(UUID.randomUUID(), category, question, answer, 1, true);
  }
}
