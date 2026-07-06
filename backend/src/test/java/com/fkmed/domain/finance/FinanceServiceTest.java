package com.fkmed.domain.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.BeneficiaryRole;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SPEC-0013: the titular-only guard (BR1), tab ordering + overdue update (BR2), validator format/
 * lookup (BR4), copay total (BR5), IR year selection (BR6) and settlement-year eligibility (BR7).
 * Domain/unit layer, mocking the repositories and the plan facade (mirrors {@code
 * ClinicalDocumentServiceTest}).
 */
@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

  private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZONE);
  private static final String MARIA_CARD = "001234567";
  private static final String PEDRO_CARD = "001234575";
  private static final UUID MARIA_ID = UUID.randomUUID();
  private static final UUID PEDRO_ID = UUID.randomUUID();
  private static final LocalDate TODAY = LocalDate.of(2026, 7, 6);
  private static final String L1 = "23793381286000826010494120780301189999000000001";
  private static final String L2 = "23791762053000512038594876120345672000000000002";
  private static final String L3 = "34191098765000432019874561230987650000000000003";
  private static final String L4 = "00190500954014481606906809350888888000000000004";

  @Mock private InvoiceRepository invoiceRepository;
  @Mock private CopayEntryRepository copayRepository;
  @Mock private BeneficiaryAccess beneficiaryAccess;

  private FinanceService service;
  private SimpleMeterRegistry metrics;

  @BeforeEach
  void setUp() {
    metrics = new SimpleMeterRegistry();
    service =
        new FinanceService(invoiceRepository, copayRepository, beneficiaryAccess, CLOCK, metrics);
    lenient()
        .when(beneficiaryAccess.accessibleFor(MARIA_CARD))
        .thenReturn(
            List.of(
                new AccessibleBeneficiary(MARIA_ID, "MARIA", BeneficiaryRole.TITULAR),
                new AccessibleBeneficiary(PEDRO_ID, "PEDRO", BeneficiaryRole.DEPENDENT)));
  }

  private Invoice invoice(LocalDate competencia, LocalDate dueDate, String line, boolean paid) {
    Invoice invoice =
        Invoice.issue(
            MARIA_ID,
            competencia,
            dueDate,
            new BigDecimal("489.90"),
            line,
            "pix",
            Instant.parse("2026-01-01T00:00:00Z"));
    if (paid) {
      invoice.markPaid(Instant.parse("2025-06-01T12:00:00Z"));
    }
    return invoice;
  }

  private List<Invoice> seededInvoices() {
    return List.of(
        invoice(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 10), L1, true),
        invoice(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 16), L3, false), // OPEN
        invoice(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), L4, false), // OVERDUE
        invoice(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 10), L2, true));
  }

  @Test
  void aDependentCaller_isRejectedWith403() {
    when(beneficiaryAccess.accessibleFor(PEDRO_CARD))
        .thenReturn(
            List.of(new AccessibleBeneficiary(PEDRO_ID, "PEDRO", BeneficiaryRole.DEPENDENT)));

    assertThatExceptionOfType(FinanceTitularOnlyException.class)
        .isThrownBy(() -> service.invoices(PEDRO_CARD, InvoiceTab.OPEN));
    assertThatExceptionOfType(FinanceTitularOnlyException.class)
        .isThrownBy(() -> service.copay(PEDRO_CARD, TODAY.minusMonths(1), TODAY, null));
  }

  @Test
  void anUnknownCard_isRejectedWith403() {
    when(beneficiaryAccess.accessibleFor("000000000")).thenReturn(List.of());

    assertThatExceptionOfType(FinanceTitularOnlyException.class)
        .isThrownBy(() -> service.invoices("000000000", InvoiceTab.OPEN));
  }

  @Test
  void openTab_ordersByDueDateAscending_overdueFirst_withTheUpdatedAmount() {
    when(invoiceRepository.findByTitularBeneficiaryId(MARIA_ID)).thenReturn(seededInvoices());

    List<InvoiceSummary> open = service.invoices(MARIA_CARD, InvoiceTab.OPEN);

    assertThat(open).hasSize(2);
    assertThat(open.get(0).status()).isEqualTo(InvoiceStatus.OVERDUE);
    assertThat(open.get(0).updatedAmount()).isNotNull();
    // 2026-06-30 -> 2026-07-06 = 6 days overdue.
    assertThat(open.get(0).updatedAmount().daysOverdue()).isEqualTo(6);
    assertThat(open.get(1).status()).isEqualTo(InvoiceStatus.OPEN);
    assertThat(open.get(1).updatedAmount()).isNull();
    assertThat(open.get(1).paidAt()).isNull();
  }

  @Test
  void paidTab_ordersByCompetenciaDescending_withThePaymentDate() {
    when(invoiceRepository.findByTitularBeneficiaryId(MARIA_ID)).thenReturn(seededInvoices());

    List<InvoiceSummary> paid = service.invoices(MARIA_CARD, InvoiceTab.PAID);

    assertThat(paid).hasSize(2);
    assertThat(paid.get(0).competencia()).isEqualTo("Setembro/2025");
    assertThat(paid.get(1).competencia()).isEqualTo("Maio/2025");
    assertThat(paid.get(0).status()).isEqualTo(InvoiceStatus.PAID);
    assertThat(paid.get(0).paidAt()).isNotNull();
    assertThat(paid.get(0).updatedAmount()).isNull();
  }

  @Test
  void validate_rejectsANon47DigitLine_beforeAnyLookup() {
    assertThatExceptionOfType(LineInvalidFormatException.class)
        .isThrownBy(() -> service.validate(MARIA_CARD, "123456789012345678901234567890"));
  }

  @Test
  void validate_reportsAuthentic_forAnIssuedLine_evenWithSpaces() {
    when(invoiceRepository.findByDigitableLine(L1))
        .thenReturn(
            Optional.of(invoice(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 10), L1, true)));

    InvoiceValidation result =
        service.validate(MARIA_CARD, "23793.38128 60008.260104 94120.780301 1 89999000000001");

    assertThat(result.result()).isEqualTo("AUTHENTIC");
    assertThat(result.competencia()).isEqualTo("Maio/2025");
    assertThat(result.amount()).isEqualByComparingTo("489.90");
  }

  @Test
  void validate_reportsNotRecognized_forAnUnknownLine_withNoData() {
    when(invoiceRepository.findByDigitableLine(L4)).thenReturn(Optional.empty());

    InvoiceValidation result = service.validate(MARIA_CARD, L4);

    assertThat(result.result()).isEqualTo("NOT_RECOGNIZED");
    assertThat(result.competencia()).isNull();
    assertThat(result.amount()).isNull();
  }

  @Test
  void validate_countsUsesByResult_theAntifraudFraudSignal() {
    // SPEC-0013 §Observability: the validator counter split by result (fresh-eyes review, slice
    // 5.2).
    when(invoiceRepository.findByDigitableLine(L1))
        .thenReturn(
            Optional.of(invoice(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 10), L1, true)));
    when(invoiceRepository.findByDigitableLine(L4)).thenReturn(Optional.empty());

    service.validate(MARIA_CARD, L1);
    service.validate(MARIA_CARD, L4);

    assertThat(metrics.counter("finance.validator", "result", "authentic").count()).isEqualTo(1.0);
    assertThat(metrics.counter("finance.validator", "result", "not_recognized").count())
        .isEqualTo(1.0);
  }

  @Test
  void taxStatementYears_areOnlyYearsWithPayments_descending() {
    when(invoiceRepository.findByTitularBeneficiaryId(MARIA_ID)).thenReturn(seededInvoices());

    assertThat(service.taxStatementYears(MARIA_CARD))
        .extracting(StatementYear::year)
        .containsExactly(2025);
  }

  @Test
  void settlementYears_areOnlyFullyPaidYears_descending() {
    when(invoiceRepository.findByTitularBeneficiaryId(MARIA_ID)).thenReturn(seededInvoices());

    // 2025 fully paid; 2026 has open + overdue.
    assertThat(service.settlementYears(MARIA_CARD))
        .extracting(StatementYear::year)
        .containsExactly(2025);
  }

  @Test
  void settlementPdf_forAnotFullyPaidYear_isRejectedWith409() {
    when(invoiceRepository.findByTitularBeneficiaryId(MARIA_ID)).thenReturn(seededInvoices());

    assertThatExceptionOfType(YearNotSettledException.class)
        .isThrownBy(() -> service.settlementPdf(MARIA_CARD, 2026));
  }

  @Test
  void copay_totalsExactlyTheReturnedEntries() {
    CopayEntry a =
        CopayEntry.record(
            TODAY.minusDays(3), "Consulta", "Clínica A", MARIA_ID, new BigDecimal("35.00"));
    CopayEntry b =
        CopayEntry.record(TODAY.minusDays(10), "Exame", "Lab B", PEDRO_ID, new BigDecimal("18.50"));
    when(copayRepository.findByBeneficiaryIdInAndEntryDateBetweenOrderByEntryDateDesc(
            org.mockito.ArgumentMatchers.anyCollection(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(a, b));

    CopayStatement statement = service.copay(MARIA_CARD, TODAY.minusMonths(1), TODAY, null);

    assertThat(statement.entries()).hasSize(2);
    assertThat(statement.total()).isEqualByComparingTo("53.50");
    assertThat(statement.entries().get(0).beneficiaryName()).isEqualTo("MARIA");
  }

  @Test
  void copay_withABeneficiaryFilterOutsideTheFamily_isEmpty() {
    CopayStatement statement =
        service.copay(MARIA_CARD, TODAY.minusMonths(1), TODAY, UUID.randomUUID());

    assertThat(statement.entries()).isEmpty();
    assertThat(statement.total()).isEqualByComparingTo("0.00");
  }
}
