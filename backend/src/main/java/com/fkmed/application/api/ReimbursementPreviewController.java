package com.fkmed.application.api;

import com.fkmed.application.api.dto.ReimbursementDocumentRequest;
import com.fkmed.application.api.dto.ReimbursementPreviewRequest;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.reimbursement.PreviewNotFoundException;
import com.fkmed.domain.reimbursement.ReimbursementDocumentRequiredException;
import com.fkmed.domain.reimbursement.ReimbursementPreviewListItem;
import com.fkmed.domain.reimbursement.ReimbursementPreviewResult;
import com.fkmed.domain.reimbursement.ReimbursementPreviewService;
import com.fkmed.domain.reimbursement.UploadedDocument;
import com.fkmed.infra.security.UserContextProvider;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Beneficiary-facing reimbursement preview endpoints (SPEC-0017). */
@RestController
@RequestMapping("/api/reimbursement-previews")
@RequiredArgsConstructor
public class ReimbursementPreviewController {

  private final ReimbursementPreviewService previews;
  private final UserContextProvider userContext;
  private final IdentityAccounts accounts;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  ReimbursementPreviewResult create(
      @Valid @RequestPart("request") ReimbursementPreviewRequest request,
      @RequestPart(value = "documents", required = false) List<MultipartFile> documents) {
    return previews.create(
        callerCard(),
        authorAccountId(),
        request.beneficiaryId(),
        request.expenseTypeCode(),
        uploadedDocuments(request.documents(), documents));
  }

  @GetMapping
  List<ReimbursementPreviewListItem> list(
      @RequestParam(value = "beneficiaryId", required = false) UUID beneficiaryId) {
    return previews.list(callerCard(), beneficiaryId);
  }

  @GetMapping("/{id}")
  ReimbursementPreviewResult detail(@PathVariable UUID id) {
    return previews.detail(callerCard(), id);
  }

  private String callerCard() {
    return userContext.current().beneficiaryCard().orElse(null);
  }

  private UUID authorAccountId() {
    return accounts
        .findByEmail(userContext.current().username())
        .map(AccountCredentials::accountId)
        .orElseThrow(PreviewNotFoundException::new);
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
      throw new UncheckedIOException("could not read the uploaded preview document", e);
    }
  }
}
