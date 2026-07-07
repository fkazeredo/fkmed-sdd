package com.fkmed.application.api;

import com.fkmed.application.api.dto.ValidateInvoiceRequest;
import com.fkmed.domain.finance.CopayPeriod;
import com.fkmed.domain.finance.CopayStatement;
import com.fkmed.domain.finance.FinanceService;
import com.fkmed.domain.finance.InvoiceDetail;
import com.fkmed.domain.finance.InvoiceSummary;
import com.fkmed.domain.finance.InvoiceTab;
import com.fkmed.domain.finance.InvoiceValidation;
import com.fkmed.domain.finance.StatementYear;
import com.fkmed.infra.security.UserContextProvider;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Finance endpoints (SPEC-0013 — Plano › Finanças): invoice list/detail/second-copy PDF, the
 * antifraud validator, the copay statement and the IR/settlement declarations. Every route is
 * titular-only (BR1) — the caller's beneficiary card is resolved from the JWT and the titular check
 * lives in {@code domain.finance.FinanceService} (403 {@code finance.titular-only} for a
 * dependent). The copay-period query shape (a code or a custom range) is a request-parsing concern
 * resolved here, mirroring {@code ClinicalDocumentController}.
 */
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

  private final FinanceService finance;
  private final UserContextProvider userContext;
  private final Clock clock;

  /** BR2: the OPEN (open + overdue) or PAID tab of the titular's invoices. */
  @GetMapping("/invoices")
  List<InvoiceSummary> invoices(@RequestParam(defaultValue = "OPEN") InvoiceTab tab) {
    return finance.invoices(callerCard(), tab);
  }

  /**
   * BR3: the invoice detail (summary + payment identifiers); 404 when unknown or out of contract.
   */
  @GetMapping("/invoices/{id}")
  InvoiceDetail invoice(@PathVariable UUID id) {
    return finance.invoiceDetail(callerCard(), id);
  }

  /** BR3: the invoice's second-copy PDF (a paid invoice carries the "PAGO" watermark). */
  @GetMapping(value = "/invoices/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  ResponseEntity<byte[]> invoicePdf(@PathVariable UUID id) {
    return pdf(finance.invoicePdf(callerCard(), id), "boleto-" + id + ".pdf");
  }

  /**
   * BR4: the antifraud validator — normalizes then requires exactly 47 digits before any lookup.
   */
  @PostMapping("/invoices/validate")
  InvoiceValidation validate(@Valid @RequestBody ValidateInvoiceRequest request) {
    return finance.validate(callerCard(), request.line());
  }

  /** BR5: the copay statement for the period (and optional single beneficiary) with its total. */
  @GetMapping("/copay")
  CopayStatement copay(
      @RequestParam(defaultValue = "CURRENT_MONTH") CopayPeriod period,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) UUID beneficiaryId) {
    LocalDate[] range = resolvePeriod(period, from, to);
    return finance.copay(callerCard(), range[0], range[1], beneficiaryId);
  }

  /** BR6: the base years with payments (IR statements). */
  @GetMapping("/tax-statements")
  List<StatementYear> taxStatements() {
    return finance.taxStatementYears(callerCard());
  }

  /** BR6: the IR statement PDF for a base year. */
  @GetMapping(value = "/tax-statements/{year}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  ResponseEntity<byte[]> taxStatementPdf(@PathVariable int year) {
    return pdf(finance.taxStatementPdf(callerCard(), year), "ir-" + year + ".pdf");
  }

  /** BR7: the fully-paid base years (Lei 12.007 settlement declarations). */
  @GetMapping("/settlement-declarations")
  List<StatementYear> settlementDeclarations() {
    return finance.settlementYears(callerCard());
  }

  /**
   * BR7: the settlement declaration PDF for a fully-paid year; 409 when the year is not settled.
   */
  @GetMapping(
      value = "/settlement-declarations/{year}/pdf",
      produces = MediaType.APPLICATION_PDF_VALUE)
  ResponseEntity<byte[]> settlementPdf(@PathVariable int year) {
    return pdf(finance.settlementPdf(callerCard(), year), "quitacao-" + year + ".pdf");
  }

  private String callerCard() {
    return userContext.current().beneficiaryCard().orElse(null);
  }

  private LocalDate[] resolvePeriod(CopayPeriod period, LocalDate from, LocalDate to) {
    LocalDate today = LocalDate.now(clock);
    return switch (period) {
      case CURRENT_MONTH -> new LocalDate[] {today.withDayOfMonth(1), today};
      case LAST_3M -> new LocalDate[] {today.minusMonths(3), today};
      case CUSTOM -> customRange(from, to);
    };
  }

  private static LocalDate[] customRange(LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "the 'CUSTOM' period requires both 'from' and 'to'");
    }
    if (to.isBefore(from)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'to' must not be before 'from'");
    }
    return new LocalDate[] {from, to};
  }

  private static ResponseEntity<byte[]> pdf(byte[] body, String filename) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(body);
  }
}
