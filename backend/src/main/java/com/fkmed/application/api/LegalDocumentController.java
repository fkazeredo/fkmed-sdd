package com.fkmed.application.api;

import com.fkmed.application.api.dto.AcceptLegalDocumentRequest;
import com.fkmed.domain.identity.LegalDocumentView;
import com.fkmed.domain.identity.LegalDocuments;
import com.fkmed.domain.identity.LegalDocumentsView;
import com.fkmed.infra.security.UserContextProvider;
import com.fkmed.infra.web.HttpRequestMetadata;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legal-document endpoints (SPEC-0006 BR8): the current Terms/Privacy versions with the caller's
 * acceptance state (drives the re-acceptance interception), each page's full text, and recording an
 * acceptance of the current version. The {@code {type}} path is constrained to {@code
 * TERMS|PRIVACY} so an unknown type is a routing 404, not a business error.
 */
@RestController
@RequestMapping("/api/legal-documents")
@RequiredArgsConstructor
public class LegalDocumentController {

  private final LegalDocuments legalDocuments;
  private final UserContextProvider userContext;

  /** Current Terms/Privacy versions + my acceptance state (SPEC-0006 §API Contracts). */
  @GetMapping("/current")
  LegalDocumentsView current() {
    return legalDocuments.current(userContext.current().username());
  }

  /** The full current document (version, date, body) of {@code type} + my acceptance state. */
  @GetMapping("/{type:TERMS|PRIVACY}")
  LegalDocumentView document(@PathVariable String type) {
    return legalDocuments.document(userContext.current().username(), type);
  }

  /** Records acceptance of the current version; 409 {@code legal.version-outdated} if stale. */
  @PostMapping("/{type:TERMS|PRIVACY}/accept")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void accept(@PathVariable String type, @Valid @RequestBody AcceptLegalDocumentRequest request) {
    legalDocuments.accept(
        userContext.current().username(), type, request.version(), HttpRequestMetadata.current());
  }
}
