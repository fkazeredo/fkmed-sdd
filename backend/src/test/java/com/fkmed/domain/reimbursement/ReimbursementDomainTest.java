package com.fkmed.domain.reimbursement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReimbursementDomainTest {

  private static final Instant NOW = Instant.parse("2026-07-08T12:00:00Z");
  private static final UUID BENEFICIARY = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");
  private static final UUID AUTHOR = UUID.fromString("d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70");

  @Test
  void businessDays_skipWeekends() {
    assertThat(BusinessDays.plus(LocalDate.of(2026, 7, 3), 0)).isEqualTo(LocalDate.of(2026, 7, 3));
    assertThat(BusinessDays.plus(LocalDate.of(2026, 7, 3), 1)).isEqualTo(LocalDate.of(2026, 7, 6));
    assertThat(BusinessDays.plus(LocalDate.of(2026, 7, 3), 5)).isEqualTo(LocalDate.of(2026, 7, 10));
  }

  @Test
  void documentCheckDigits_acceptsCpfAndCnpjAndRejectsInvalidDocuments() {
    assertThat(DocumentCheckDigits.isValid("39053344705")).isTrue();
    assertThat(DocumentCheckDigits.isValid("11222333000181")).isTrue();

    assertThat(DocumentCheckDigits.isValid(null)).isFalse();
    assertThat(DocumentCheckDigits.isValid("11111111111")).isFalse();
    assertThat(DocumentCheckDigits.isValid("39053344706")).isFalse();
    assertThat(DocumentCheckDigits.isValid("11222333000182")).isFalse();
    assertThat(DocumentCheckDigits.isValid("123")).isFalse();
  }

  @Test
  void calculation_capsEverySessionAndComputesGlosa() {
    ReimbursementService service =
        new ReimbursementService(
            null, null, null, null, null, null, null, null, null, null, null, null, clock());
    ReimbursementTableEntry table = tableEntry("TERAPIA", "60.00", true, "1.00");

    ReimbursementCalculation calculation =
        service.calculate(
            table,
            money("400.00"),
            List.of(
                SessionItem.of(null, LocalDate.of(2026, 6, 1), money("100.00")),
                SessionItem.of(null, LocalDate.of(2026, 6, 8), money("100.00")),
                SessionItem.of(null, LocalDate.of(2026, 6, 15), money("100.00")),
                SessionItem.of(null, LocalDate.of(2026, 6, 20), money("100.00"))));

    assertThat(calculation.amountReimbursed()).isEqualByComparingTo("240.00");
    assertThat(calculation.glosaAmount()).isEqualByComparingTo("160.00");
    assertThat(calculation.glosaReason()).isEqualTo("Valor excede a tabela do plano");
  }

  @Test
  void calculation_withoutExcessHasNoGlosaReason() {
    ReimbursementCalculation calculation =
        ReimbursementCalculation.of(money("120.00"), money("120.00"));

    assertThat(calculation.amountReimbursed()).isEqualByComparingTo("120.00");
    assertThat(calculation.glosaAmount()).isEqualByComparingTo("0.00");
    assertThat(calculation.glosaReason()).isNull();
  }

  @Test
  void stateMachine_rejectsInvalidTransitionsAndKeepsPaymentIdempotent() {
    ReimbursementRequest request = submittedRequest();

    request.markProcessing(
        ReimbursementCalculation.of(money("120.00"), money("150.00")),
        LocalDate.of(2026, 7, 15),
        NOW.plusSeconds(1));
    assertThatThrownBy(() -> request.pay(NOW.plusSeconds(2)))
        .isInstanceOf(ReimbursementInvalidTransitionException.class);

    request.approve(NOW.plusSeconds(3));
    assertThat(request.pay(NOW.plusSeconds(4))).isTrue();
    assertThat(request.pay(NOW.plusSeconds(5))).isFalse();

    assertThat(request.getStatus()).isEqualTo(ReimbursementStatus.PAGO);
    assertThat(request.getTimelineEvents())
        .extracting(TimelineEvent::getStatus)
        .containsExactly(
            ReimbursementStatus.EM_ANALISE,
            ReimbursementStatus.PROCESSAMENTO,
            ReimbursementStatus.APROVADO,
            ReimbursementStatus.PAGO);
  }

  @Test
  void stateMachine_resolvesPendencyBackToProcessing() {
    ReimbursementRequest request = submittedRequest();

    request.openPendency("Enviar pedido medico", LocalDate.of(2026, 8, 7), NOW.plusSeconds(1));
    request.resolvePendency(
        List.of(stored(DocumentCategory.MEDICAL_ORDER, "p.pdf")),
        ReimbursementCalculation.of(money("120.00"), money("150.00")),
        LocalDate.of(2026, 7, 15),
        NOW.plusSeconds(2));

    assertThat(request.getStatus()).isEqualTo(ReimbursementStatus.PROCESSAMENTO);
    assertThat(request.getPendencyDescription()).isNull();
    assertThat(request.getPendencyDeadlineAt()).isNull();
    assertThat(request.getDocuments()).hasSize(2);
    assertThat(request.getTimelineEvents())
        .extracting(TimelineEvent::getStatus)
        .containsExactly(
            ReimbursementStatus.EM_ANALISE,
            ReimbursementStatus.PENDENTE_DOCUMENTACAO,
            ReimbursementStatus.PROCESSAMENTO);
  }

  @Test
  void stateMachine_deniesWithReasonAndZeroesReimbursedAmount() {
    ReimbursementRequest request = submittedRequest();
    request.markProcessing(
        ReimbursementCalculation.of(money("120.00"), money("150.00")),
        LocalDate.of(2026, 7, 15),
        NOW.plusSeconds(1));

    assertThatThrownBy(() -> request.deny(" ", NOW.plusSeconds(2)))
        .isInstanceOf(ReimbursementInvalidTransitionException.class);

    request.deny("Recibo sem identificacao do prestador", NOW.plusSeconds(3));

    assertThat(request.getStatus()).isEqualTo(ReimbursementStatus.NEGADO);
    assertThat(request.getDenialReason()).isEqualTo("Recibo sem identificacao do prestador");
    assertThat(request.getAmountReimbursed()).isEqualByComparingTo("0.00");
  }

  @Test
  void stateMachine_paymentFailureCanBeCorrectedAndPaid() {
    ReimbursementRequest request = submittedRequest();
    request.markProcessing(
        ReimbursementCalculation.of(money("120.00"), money("150.00")),
        LocalDate.of(2026, 7, 15),
        NOW.plusSeconds(1));
    request.approve(NOW.plusSeconds(2));
    request.failPayment("Conta encerrada", NOW.plusSeconds(3));

    assertThat(request.getStatus()).isEqualTo(ReimbursementStatus.PAGAMENTO_NAO_EFETUADO);
    assertThat(request.getPaymentFailureReason()).isEqualTo("Conta encerrada");

    request.correctBankAndPay(
        "237",
        "4321",
        "999888777",
        "1",
        BankAccountType.POUPANCA,
        LocalDate.of(2026, 7, 16),
        NOW.plusSeconds(4));

    assertThat(request.getStatus()).isEqualTo(ReimbursementStatus.PAGO);
    assertThat(request.getBankCode()).isEqualTo("237");
    assertThat(request.maskedBankAccount()).isEqualTo("...8777");
    assertThat(request.getPaymentFailureReason()).isNull();
  }

  @Test
  void stateMachine_cancelsOnlyOpenPendencies() {
    ReimbursementRequest request = submittedRequest();

    assertThatThrownBy(() -> request.cancelByPendencyExpiry(NOW.plusSeconds(1)))
        .isInstanceOf(ReimbursementInvalidTransitionException.class);

    request.openPendency("Enviar recibo legivel", LocalDate.of(2026, 8, 7), NOW.plusSeconds(2));
    request.cancelByPendencyExpiry(NOW.plusSeconds(3));

    assertThat(request.getStatus()).isEqualTo(ReimbursementStatus.CANCELADO);
    assertThat(request.getTimelineEvents())
        .extracting(TimelineEvent::getStatus)
        .containsExactly(
            ReimbursementStatus.EM_ANALISE,
            ReimbursementStatus.PENDENTE_DOCUMENTACAO,
            ReimbursementStatus.CANCELADO);
  }

  @Test
  void preview_canBeImmediateOrAnalyzedAndConclusionIsIdempotent() {
    ReimbursementPreview immediate =
        ReimbursementPreview.immediate(
            "PV-20260708-0001",
            BENEFICIARY,
            ExpenseTypeCodes.CONSULTA,
            money("120.00"),
            AUTHOR,
            NOW);

    assertThat(immediate.getSituation()).isEqualTo(PreviewSituation.CONCLUIDA);
    assertThat(immediate.getEstimatedValue()).isEqualByComparingTo("120.00");
    assertThat(immediate.getConcludedAt()).isEqualTo(NOW);

    ReimbursementPreview analyzed =
        ReimbursementPreview.analyzed(
            "PV-20260708-0002",
            BENEFICIARY,
            ExpenseTypeCodes.EXAME,
            List.of(
                stored(DocumentCategory.BUDGET, "o.pdf"),
                stored(DocumentCategory.MEDICAL_ORDER, "p.pdf")),
            AUTHOR,
            NOW);

    assertThat(analyzed.getSituation()).isEqualTo(PreviewSituation.EM_ANALISE);
    assertThat(analyzed.getDocuments())
        .extracting(PreviewDocument::getFileSize)
        .containsExactly(4, 4);

    analyzed.conclude(money("80.00"), NOW.plusSeconds(10));
    analyzed.conclude(money("90.00"), NOW.plusSeconds(20));

    assertThat(analyzed.getSituation()).isEqualTo(PreviewSituation.CONCLUIDA);
    assertThat(analyzed.getEstimatedValue()).isEqualByComparingTo("90.00");
    assertThat(analyzed.getConcludedAt()).isEqualTo(NOW.plusSeconds(10));
  }

  private static ReimbursementRequest submittedRequest() {
    return ReimbursementRequest.submit(
        new SubmitReimbursementCommand(
            BENEFICIARY,
            ExpenseTypeCodes.CONSULTA,
            LocalDate.of(2026, 6, 8),
            money("150.00"),
            List.of(),
            "Clinica Livre Escolha",
            "CRM",
            "123456",
            "RJ",
            "39053344705",
            "Clinica medica",
            "001",
            "1234",
            "123456",
            "7",
            BankAccountType.CORRENTE,
            "1.0",
            List.of(
                new UploadedDocument(DocumentCategory.RECEIPT, pdf(), "application/pdf", "r.pdf")),
            "it-domain"),
        "RE-20260708-0001",
        term(),
        LocalDate.of(2026, 7, 15),
        AUTHOR,
        List.of(stored(DocumentCategory.RECEIPT, "r.pdf")),
        NOW);
  }

  private static StoredDocument stored(DocumentCategory category, String fileName) {
    return new StoredDocument(
        category,
        "postgres:reimbursement-document/12345678-1234-4234-8234-123456789abc",
        "application/pdf",
        fileName,
        pdf().length);
  }

  private static AdhesionTerm term() {
    AdhesionTerm term = new AdhesionTerm();
    ReflectionTestUtils.setField(term, "version", "1.0");
    return term;
  }

  private static ReimbursementTableEntry tableEntry(
      String code, String amount, boolean perSession, String multiple) {
    ReimbursementTableEntry entry = new ReimbursementTableEntry();
    ReflectionTestUtils.setField(entry, "expenseTypeCode", code);
    ReflectionTestUtils.setField(entry, "amount", money(amount));
    ReflectionTestUtils.setField(entry, "perSession", perSession);
    ReflectionTestUtils.setField(entry, "planMultiple", money(multiple));
    return entry;
  }

  private static BigDecimal money(String value) {
    return new BigDecimal(value);
  }

  private static byte[] pdf() {
    return new byte[] {0x25, 0x50, 0x44, 0x46};
  }

  private static Clock clock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }
}
