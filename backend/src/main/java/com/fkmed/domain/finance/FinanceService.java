package com.fkmed.domain.finance;

import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.BeneficiaryRole;
import com.fkmed.domain.plan.BeneficiarySummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service of the finance module's read-only API (SPEC-0013), exclusive to the contract
 * titular (BR1). Every entry point first resolves the caller's family scope through {@link
 * BeneficiaryAccess#accessibleFor} and rejects a non-titular with {@link
 * FinanceTitularOnlyException} (403). Invoice status and the overdue update are DERIVED at read
 * time against the product clock (BR2); the IR statement and settlement declaration are derived
 * from the invoices, no table of their own (BR6/BR7).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceService {

  private final InvoiceRepository invoices;
  private final CopayEntryRepository copayEntries;
  private final BeneficiaryAccess beneficiaryAccess;
  private final Clock clock;
  private final MeterRegistry metrics;

  /** The invoices of the titular's contract for {@code tab} (BR2), ordered per tab. */
  public List<InvoiceSummary> invoices(String callerCard, InvoiceTab tab) {
    UUID titularId = requireTitular(callerCard).beneficiaryId();
    LocalDate today = LocalDate.now(clock);
    List<Invoice> found = invoices.findByTitularBeneficiaryId(titularId);
    Comparator<Invoice> order =
        tab == InvoiceTab.OPEN
            ? Comparator.comparing(Invoice::getDueDate)
            : Comparator.comparing(Invoice::getCompetencia).reversed();
    return found.stream()
        .filter(invoice -> matchesTab(invoice, tab, today))
        .sorted(order)
        .map(invoice -> toSummary(invoice, today))
        .toList();
  }

  /**
   * The detail of one invoice within the titular's contract (BR3): summary + payment identifiers.
   *
   * @throws InvoiceNotFoundException when unknown or out of the caller's contract (existence never
   *     revealed).
   */
  public InvoiceDetail invoiceDetail(String callerCard, UUID invoiceId) {
    Invoice invoice = requireOwnInvoice(callerCard, invoiceId);
    LocalDate today = LocalDate.now(clock);
    InvoiceSummary summary = toSummary(invoice, today);
    return new InvoiceDetail(
        summary.id(),
        summary.competencia(),
        summary.dueDate(),
        summary.amount(),
        summary.status(),
        summary.paidAt(),
        summary.updatedAmount(),
        invoice.getDigitableLine(),
        invoice.getPixCode(),
        DigitableLine.barcodeOf(invoice.getDigitableLine()));
  }

  /** The invoice's second-copy PDF (BR3); a paid invoice's PDF carries the "PAGO" watermark. */
  public byte[] invoicePdf(String callerCard, UUID invoiceId) {
    Invoice invoice = requireOwnInvoice(callerCard, invoiceId);
    LocalDate today = LocalDate.now(clock);
    metrics.counter("finance.pdf.download", "type", "invoice").increment();
    return InvoicePdfRenderer.render(toSummary(invoice, today), invoice.getDigitableLine());
  }

  /**
   * Validates a submitted line against the issued invoices (BR4 antifraud). Normalizes first and
   * requires exactly 47 digits BEFORE any lookup; then reports AUTHENTIC (with the invoice's
   * competência/due date/amount) or NOT_RECOGNIZED.
   *
   * @throws LineInvalidFormatException when the normalized line is not exactly 47 digits.
   */
  public InvoiceValidation validate(String callerCard, String rawLine) {
    requireTitular(callerCard);
    String normalized = DigitableLine.normalize(rawLine);
    if (normalized.length() != DigitableLine.DIGITS) {
      throw new LineInvalidFormatException();
    }
    Optional<Invoice> match = invoices.findByDigitableLine(normalized);
    // SPEC-0013 §Observability: count validator uses split by result — the antifraud "fraud
    // signal" (a spike of not-recognized boletos).
    metrics
        .counter("finance.validator", "result", match.isPresent() ? "authentic" : "not_recognized")
        .increment();
    return match
        .map(
            invoice ->
                InvoiceValidation.authentic(
                    Competencia.label(invoice.getCompetencia()),
                    invoice.getDueDate(),
                    invoice.getAmount()))
        .orElseGet(InvoiceValidation::notRecognized);
  }

  /**
   * The copay statement for the whole family or one member within {@code [from, to]} (BR5). The
   * total is always the sum of exactly the returned entries; a {@code beneficiaryFilter} outside
   * the family yields an empty statement (the titular's scope is the authority).
   */
  public CopayStatement copay(
      String callerCard, LocalDate from, LocalDate to, UUID beneficiaryFilter) {
    List<AccessibleBeneficiary> family = requireTitularFamily(callerCard);
    Map<UUID, String> names =
        family.stream()
            .collect(
                Collectors.toMap(
                    AccessibleBeneficiary::beneficiaryId, AccessibleBeneficiary::firstName));
    Collection<UUID> scope;
    if (beneficiaryFilter != null) {
      scope = names.containsKey(beneficiaryFilter) ? Set.of(beneficiaryFilter) : Set.of();
    } else {
      scope = names.keySet();
    }
    if (scope.isEmpty()) {
      return new CopayStatement(List.of(), BigDecimal.ZERO.setScale(2));
    }
    List<CopayEntry> found =
        copayEntries.findByBeneficiaryIdInAndEntryDateBetweenOrderByEntryDateDesc(scope, from, to);
    List<CopayLine> lines =
        found.stream()
            .map(
                entry ->
                    new CopayLine(
                        entry.getEntryDate(),
                        entry.getProcedure(),
                        entry.getProvider(),
                        names.getOrDefault(entry.getBeneficiaryId(), ""),
                        entry.getAmount()))
            .toList();
    BigDecimal total =
        lines.stream().map(CopayLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    return new CopayStatement(lines, total);
  }

  /** The base years with payments, most recent first (BR6). */
  public List<StatementYear> taxStatementYears(String callerCard) {
    UUID titularId = requireTitular(callerCard).beneficiaryId();
    return invoices.findByTitularBeneficiaryId(titularId).stream()
        .filter(Invoice::paid)
        .map(invoice -> invoice.getCompetencia().getYear())
        .distinct()
        .sorted(Comparator.reverseOrder())
        .map(StatementYear::new)
        .toList();
  }

  /**
   * The IR statement PDF for {@code year} (BR6): the 12 monthly amounts (paid) + the annual total.
   */
  public byte[] taxStatementPdf(String callerCard, int year) {
    UUID titularId = requireTitular(callerCard).beneficiaryId();
    BeneficiarySummary titular = beneficiaryAccess.summaryFor(callerCard, titularId);
    BigDecimal[] months = new BigDecimal[12];
    for (int i = 0; i < 12; i++) {
      months[i] = BigDecimal.ZERO.setScale(2);
    }
    for (Invoice invoice : invoices.findByTitularBeneficiaryId(titularId)) {
      if (invoice.paid() && invoice.getCompetencia().getYear() == year) {
        int month = invoice.getCompetencia().getMonthValue() - 1;
        months[month] = months[month].add(invoice.getAmount());
      }
    }
    metrics.counter("finance.pdf.download", "type", "tax-statement").increment();
    return TaxStatementPdfRenderer.render(
        titular.fullName(), titular.cardNumber(), titular.planName(), year, months);
  }

  /** The fully-paid base years (Lei 12.007), most recent first (BR7). */
  public List<StatementYear> settlementYears(String callerCard) {
    UUID titularId = requireTitular(callerCard).beneficiaryId();
    return fullyPaidYears(titularId).stream()
        .sorted(Comparator.reverseOrder())
        .map(StatementYear::new)
        .toList();
  }

  /**
   * The settlement-declaration PDF for {@code year} (BR7): contract, beneficiaries, competências
   * settled, issue date.
   *
   * @throws YearNotSettledException when the base year still has open/overdue invoices.
   */
  public byte[] settlementPdf(String callerCard, int year) {
    List<AccessibleBeneficiary> family = requireTitularFamily(callerCard);
    UUID titularId = family.get(0).beneficiaryId();
    if (!fullyPaidYears(titularId).contains(year)) {
      throw new YearNotSettledException();
    }
    BeneficiarySummary titular = beneficiaryAccess.summaryFor(callerCard, titularId);
    List<String> competencias =
        invoices.findByTitularBeneficiaryId(titularId).stream()
            .filter(invoice -> invoice.getCompetencia().getYear() == year)
            .sorted(Comparator.comparing(Invoice::getCompetencia))
            .map(invoice -> Competencia.label(invoice.getCompetencia()))
            .toList();
    List<String> beneficiaries = family.stream().map(AccessibleBeneficiary::firstName).toList();
    metrics.counter("finance.pdf.download", "type", "settlement").increment();
    return SettlementDeclarationPdfRenderer.render(
        titular.fullName(),
        titular.cardNumber(),
        titular.planName(),
        year,
        beneficiaries,
        competencias,
        LocalDate.now(clock));
  }

  private Set<Integer> fullyPaidYears(UUID titularId) {
    Map<Integer, List<Invoice>> byYear =
        invoices.findByTitularBeneficiaryId(titularId).stream()
            .collect(Collectors.groupingBy(invoice -> invoice.getCompetencia().getYear()));
    return byYear.entrySet().stream()
        .filter(entry -> entry.getValue().stream().allMatch(Invoice::paid))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  private boolean matchesTab(Invoice invoice, InvoiceTab tab, LocalDate today) {
    InvoiceStatus status = invoice.status(today);
    return tab == InvoiceTab.PAID
        ? status == InvoiceStatus.PAID
        : status == InvoiceStatus.OPEN || status == InvoiceStatus.OVERDUE;
  }

  private InvoiceSummary toSummary(Invoice invoice, LocalDate today) {
    InvoiceStatus status = invoice.status(today);
    ZoneId zone = clock.getZone();
    LocalDate paidAt =
        invoice.getPaidAt() == null ? null : LocalDate.ofInstant(invoice.getPaidAt(), zone);
    UpdatedAmount updated = null;
    if (status == InvoiceStatus.OVERDUE) {
      long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
      updated = UpdatedAmount.of(invoice.getAmount(), daysOverdue);
    }
    return new InvoiceSummary(
        invoice.getId().toString(),
        Competencia.label(invoice.getCompetencia()),
        invoice.getDueDate(),
        invoice.getAmount(),
        status,
        paidAt,
        updated);
  }

  private Invoice requireOwnInvoice(String callerCard, UUID invoiceId) {
    UUID titularId = requireTitular(callerCard).beneficiaryId();
    return invoices
        .findById(invoiceId)
        .filter(invoice -> invoice.getTitularBeneficiaryId().equals(titularId))
        .orElseThrow(InvoiceNotFoundException::new);
  }

  /** The caller's own beneficiary, asserting they are the contract titular (BR1). */
  private AccessibleBeneficiary requireTitular(String callerCard) {
    return requireTitularFamily(callerCard).get(0);
  }

  /**
   * The caller's family (titular first), asserting the caller is the titular (BR1).
   *
   * @throws FinanceTitularOnlyException when the card is absent/unknown or the caller is a
   *     dependent.
   */
  private List<AccessibleBeneficiary> requireTitularFamily(String callerCard) {
    List<AccessibleBeneficiary> family = beneficiaryAccess.accessibleFor(callerCard);
    if (family.isEmpty() || family.get(0).role() != BeneficiaryRole.TITULAR) {
      throw new FinanceTitularOnlyException();
    }
    return family;
  }
}
