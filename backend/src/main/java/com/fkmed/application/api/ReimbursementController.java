package com.fkmed.application.api;

import com.fkmed.application.api.dto.ReimbursementBankCorrectionRequest;
import com.fkmed.application.api.dto.ReimbursementDocumentRequest;
import com.fkmed.application.api.dto.ReimbursementPendencyDocumentsRequest;
import com.fkmed.application.api.dto.ReimbursementSubmitRequest;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.reimbursement.AdhesionTermView;
import com.fkmed.domain.reimbursement.CatalogView;
import com.fkmed.domain.reimbursement.DocumentationGuideView;
import com.fkmed.domain.reimbursement.EligibilityView;
import com.fkmed.domain.reimbursement.ReimbursementActionResult;
import com.fkmed.domain.reimbursement.ReimbursementDetailView;
import com.fkmed.domain.reimbursement.ReimbursementDocumentRequiredException;
import com.fkmed.domain.reimbursement.ReimbursementHistoryItem;
import com.fkmed.domain.reimbursement.ReimbursementService;
import com.fkmed.domain.reimbursement.ReimbursementStatementView;
import com.fkmed.domain.reimbursement.ReimbursementStatus;
import com.fkmed.domain.reimbursement.ReimbursementSubmissionResult;
import com.fkmed.domain.reimbursement.UploadedDocument;
import com.fkmed.infra.security.UserContextProvider;
import com.fkmed.infra.web.HttpRequestMetadata;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Reimbursement endpoints (SPEC-0015): eligibility, current adhesion term, documentation guide,
 * registries and request submission. Every route after eligibility is gated server-side by the plan
 * entitlement in {@link ReimbursementService}.
 */
@RestController
@RequestMapping("/api/reimbursements")
@RequiredArgsConstructor
public class ReimbursementController {

  private final ReimbursementService reimbursements;
  private final UserContextProvider userContext;
  private final IdentityAccounts accounts;

  /** BR1: plan eligibility gate. */
  @GetMapping("/eligibility")
  EligibilityView eligibility() {
    return reimbursements.eligibility(callerCard());
  }

  /** BR3: current adhesion term. */
  @GetMapping("/term")
  AdhesionTermView term() {
    return reimbursements.term(callerCard());
  }

  /** BR9: documentation summary for the selected expense type. */
  @GetMapping("/documentation-guide")
  DocumentationGuideView documentationGuide(@RequestParam("type") String type) {
    return reimbursements.documentationGuide(callerCard(), type);
  }

  /** BR4/BR10/BR11: expense, council and bank registries. */
  @GetMapping("/catalog")
  CatalogView catalog() {
    return reimbursements.catalog(callerCard());
  }

  /** SPEC-0016 BR5: history with filters. */
  @GetMapping
  List<ReimbursementHistoryItem> history(
      @RequestParam(value = "beneficiaryId", required = false) UUID beneficiaryId,
      @RequestParam(value = "status", required = false) ReimbursementStatus status,
      @RequestParam(value = "from", required = false) LocalDate from,
      @RequestParam(value = "to", required = false) LocalDate to) {
    return reimbursements.history(callerCard(), beneficiaryId, status, from, to);
  }

  /** SPEC-0016 BR10: paid-only statement. */
  @GetMapping("/statement")
  ReimbursementStatementView statement(
      @RequestParam(value = "beneficiaryId", required = false) UUID beneficiaryId,
      @RequestParam(value = "from", required = false) LocalDate from,
      @RequestParam(value = "to", required = false) LocalDate to) {
    return reimbursements.statement(callerCard(), beneficiaryId, from, to);
  }

  /** SPEC-0016 BR5: detail with timeline and data blocks. */
  @GetMapping("/{id}")
  ReimbursementDetailView detail(@PathVariable UUID id) {
    return reimbursements.detail(callerCard(), id);
  }

  /** BR12/BR13: complete request submission with idempotency and uploads. */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  ReimbursementSubmissionResult submit(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestPart("request") ReimbursementSubmitRequest request,
      @RequestPart(value = "documents", required = false) List<MultipartFile> documents) {
    List<UploadedDocument> uploaded = uploadedDocuments(request.documents(), documents);
    return reimbursements.submit(
        callerCard(),
        authorAccountId(),
        request.toCommand(idempotencyKey, uploaded),
        HttpRequestMetadata.current());
  }

  /** SPEC-0016 BR6: resolve an open documentation pendency. */
  @PostMapping(path = "/{id}/pendency-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ReimbursementActionResult resolvePendency(
      @PathVariable UUID id,
      @Valid @RequestPart("request") ReimbursementPendencyDocumentsRequest request,
      @RequestPart(value = "documents", required = false) List<MultipartFile> documents) {
    return reimbursements.resolvePendency(
        callerCard(), id, uploadedDocuments(request.documents(), documents));
  }

  /** SPEC-0016 BR8: correct bank data after failed payment. */
  @PostMapping("/{id}/bank-correction")
  ReimbursementActionResult correctBank(
      @PathVariable UUID id, @Valid @RequestBody ReimbursementBankCorrectionRequest request) {
    return reimbursements.correctBank(callerCard(), id, request.toCommand());
  }

  private String callerCard() {
    return userContext.current().beneficiaryCard().orElse(null);
  }

  private UUID authorAccountId() {
    return accounts
        .findByEmail(userContext.current().username())
        .map(AccountCredentials::accountId)
        .orElse(null);
  }

  private static List<UploadedDocument> uploadedDocuments(
      List<ReimbursementDocumentRequest> metadata, List<MultipartFile> files) {
    List<ReimbursementDocumentRequest> entries = metadata == null ? List.of() : metadata;
    List<MultipartFile> uploaded = files == null ? List.of() : files;
    if (entries.size() != uploaded.size()) {
      throw new ReimbursementDocumentRequiredException();
    }
    java.util.ArrayList<UploadedDocument> documents = new java.util.ArrayList<>();
    for (int i = 0; i < entries.size(); i++) {
      ReimbursementDocumentRequest entry = entries.get(i);
      MultipartFile file = uploaded.get(i);
      documents.add(
          new UploadedDocument(
              entry.category(),
              bytesOf(file),
              file.getContentType(),
              entry.fileName() == null || entry.fileName().isBlank()
                  ? file.getOriginalFilename()
                  : entry.fileName()));
    }
    return documents;
  }

  private static byte[] bytesOf(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("could not read the uploaded reimbursement document", e);
    }
  }
}
