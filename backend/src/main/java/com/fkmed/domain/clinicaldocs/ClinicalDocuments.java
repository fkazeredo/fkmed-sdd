package com.fkmed.domain.clinicaldocs;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The internal issuance facade (SPEC-0011 §Business Context/BR8, ADR-0013) — the ONLY way a
 * clinical document is created. Wave 2 callers: the telemedicine session close (SPEC-0010 BR10) and
 * the operator simulation (SPEC-0018) construct an {@link IssueClinicalDocumentCommand} and call
 * {@link #issue}; no beneficiary write path exists. Stamps {@code valid_until} at issue
 * (BR4/DL-0019) and publishes {@link ClinicalDocumentIssued} inside the same transaction for the
 * notification wiring (SPEC-0004).
 */
@Service
@RequiredArgsConstructor
public class ClinicalDocuments {

  private final ClinicalDocumentRepository documents;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /**
   * Issues a new immutable document bound to {@code command}'s beneficiary and origin.
   *
   * @return the new document's id.
   * @throws IllegalArgumentException when a required common or type-specific field of {@code
   *     command} is missing — an internal-contract violation by the calling module.
   */
  @Transactional
  public UUID issue(IssueClinicalDocumentCommand command) {
    Instant now = clock.instant();
    LocalDate issuedDate = LocalDate.ofInstant(now, clock.getZone());
    ClinicalDocument document = ClinicalDocument.issue(command, now, issuedDate);
    documents.save(document);
    events.publishEvent(
        new ClinicalDocumentIssued(
            document.getId(), document.getBeneficiaryId(), document.getType(), linkFor(document)));
    return document.getId();
  }

  private static String linkFor(ClinicalDocument document) {
    return "/api/clinical-documents/" + document.getId();
  }
}
