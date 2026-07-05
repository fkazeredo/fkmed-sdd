package com.fkmed.application.api;

import com.fkmed.domain.card.CardResponse;
import com.fkmed.domain.card.CardService;
import com.fkmed.infra.security.UserContextProvider;
import com.fkmed.infra.web.HttpRequestMetadata;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Digital-card endpoints (SPEC-0007): the visual card + data sheet and its PDF download. Family
 * scope and active-status enforcement live in {@code domain.card.CardService}, which reuses
 * SPEC-0003's {@code BeneficiaryAccess} scope facade.
 */
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

  private static final String PDF_FILENAME = "carteirinha.pdf";

  private final CardService cardService;
  private final UserContextProvider userContext;

  /** The card + data sheet of {@code beneficiaryId} (BR1, BR9); 404 out of scope, 409 inactive. */
  @GetMapping("/{beneficiaryId}")
  CardResponse card(@PathVariable UUID beneficiaryId) {
    return cardService.cardFor(
        userContext.current().beneficiaryCard().orElse(null),
        userContext.current().username(),
        beneficiaryId,
        HttpRequestMetadata.current());
  }

  /** The same card as a downloadable PDF (BR3). */
  @GetMapping(value = "/{beneficiaryId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  ResponseEntity<byte[]> cardPdf(@PathVariable UUID beneficiaryId) {
    byte[] pdf =
        cardService.cardPdfFor(
            userContext.current().beneficiaryCard().orElse(null),
            userContext.current().username(),
            beneficiaryId,
            HttpRequestMetadata.current());
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + PDF_FILENAME + "\"")
        .body(pdf);
  }
}
