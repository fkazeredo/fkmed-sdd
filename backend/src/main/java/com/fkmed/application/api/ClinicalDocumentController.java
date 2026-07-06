package com.fkmed.application.api;

import com.fkmed.domain.clinicaldocs.ClinicalDocumentDetail;
import com.fkmed.domain.clinicaldocs.ClinicalDocumentListResponse;
import com.fkmed.domain.clinicaldocs.ClinicalDocumentService;
import com.fkmed.domain.clinicaldocs.ClinicalDocumentType;
import com.fkmed.domain.clinicaldocs.DocumentPeriod;
import com.fkmed.infra.security.UserContextProvider;
import com.fkmed.infra.web.HttpRequestMetadata;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Clinical-document endpoints (SPEC-0011 — Minha Saúde): the filtered list, type-specific detail
 * and PDF download. Read-only for beneficiaries — family scope, the BR9 dependent-access audit and
 * the immutability guarantee all live in {@code domain.clinicaldocs.ClinicalDocumentService}; the
 * caller's beneficiary card and acting account are resolved from the JWT, never client-supplied.
 * The {@code period} query shape (a code or a custom range) is a request-parsing concern, so it is
 * resolved here rather than in the domain service — mirrors {@code AppointmentController#scope}.
 */
@RestController
@RequestMapping("/api/clinical-documents")
@RequiredArgsConstructor
public class ClinicalDocumentController {

  private static final String PDF_FILENAME = "documento.pdf";
  private static final String ALL = "all";

  private final ClinicalDocumentService documents;
  private final UserContextProvider userContext;
  private final Clock clock;

  /**
   * The Minha Saúde list (BR2): {@code category} narrows to one document type (omitted = every
   * type); {@code beneficiaryId} is {@code "all"} (default, BR2's "todos") or a specific
   * beneficiary's id; {@code period} is one of {@code P30D}/{@code P90D}/{@code P365D}/{@code
   * custom} ({@code from}/{@code to} required only for {@code custom}).
   */
  @GetMapping
  ClinicalDocumentListResponse list(
      @RequestParam(required = false) ClinicalDocumentType category,
      @RequestParam(defaultValue = ALL) String beneficiaryId,
      @RequestParam String period,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return documents.list(
        callerCard(),
        authorEmail(),
        category,
        parseBeneficiaryFilter(beneficiaryId),
        resolvePeriod(period, from, to),
        HttpRequestMetadata.current());
  }

  /** The type-specific detail (BR6); 404 when unknown or out of the caller's family scope. */
  @GetMapping("/{id}")
  ClinicalDocumentDetail detail(@PathVariable UUID id) {
    return documents.detail(callerCard(), authorEmail(), id, HttpRequestMetadata.current());
  }

  /** The same document as a faithful, downloadable PDF (BR7). */
  @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
    byte[] pdf = documents.pdfFor(callerCard(), authorEmail(), id, HttpRequestMetadata.current());
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + PDF_FILENAME + "\"")
        .body(pdf);
  }

  private String callerCard() {
    return userContext.current().beneficiaryCard().orElse(null);
  }

  private String authorEmail() {
    return userContext.current().username();
  }

  private static UUID parseBeneficiaryFilter(String beneficiaryId) {
    if (beneficiaryId == null || beneficiaryId.isBlank() || ALL.equalsIgnoreCase(beneficiaryId)) {
      return null;
    }
    try {
      return UUID.fromString(beneficiaryId);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "invalid 'beneficiaryId' — expected 'all' or a UUID");
    }
  }

  private DocumentPeriod resolvePeriod(String period, LocalDate from, LocalDate to) {
    LocalDate today = LocalDate.now(clock);
    return switch (period) {
      case "P30D" -> new DocumentPeriod(today.minusDays(30), today);
      case "P90D" -> new DocumentPeriod(today.minusDays(90), today);
      case "P365D" -> new DocumentPeriod(today.minusDays(365), today);
      case "custom" -> customPeriod(from, to);
      default ->
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "unknown 'period' — expected P30D, P90D, P365D or custom");
    };
  }

  private static DocumentPeriod customPeriod(LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "the 'custom' period requires both 'from' and 'to'");
    }
    try {
      return new DocumentPeriod(from, to);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }
}
