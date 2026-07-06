package com.fkmed.domain.identity;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public facade of the legal-document area (SPEC-0006 BR8), extending the acceptance area SPEC-0002
 * started (Rule Zero — no new module, so it reuses the identity-internal {@code term_acceptance}
 * directly rather than a cross-module port). Serves the current versions and each page's text, and
 * records immutable, versioned acceptances that share history with first-access acceptances via
 * {@link LegalDocumentTypes#acceptanceCodeFor}.
 */
@Service
@RequiredArgsConstructor
public class LegalDocuments {

  private final LegalDocumentRepository documents;
  private final TermAcceptanceRepository acceptances;
  private final UserAccountRepository accounts;
  private final AuditRecorder auditRecorder;
  private final Clock clock;

  /** Current Terms/Privacy versions with the caller's acceptance state (interception source). */
  @Transactional(readOnly = true)
  public LegalDocumentsView current(String accountEmail) {
    UUID accountId = accountId(accountEmail);
    return new LegalDocumentsView(
        statusFor(LegalDocumentTypes.TERMS, accountId),
        statusFor(LegalDocumentTypes.PRIVACY, accountId));
  }

  /** The full current document of {@code apiType} (version, date, body) plus acceptance state. */
  @Transactional(readOnly = true)
  public LegalDocumentView document(String accountEmail, String apiType) {
    UUID accountId = accountId(accountEmail);
    LegalDocument current = currentDocument(apiType);
    return new LegalDocumentView(
        apiType,
        current.getVersion(),
        current.getPublishedAt(),
        current.getBody(),
        isAccepted(accountId, apiType, current.getVersion()));
  }

  /**
   * Records the caller's acceptance of a document version (SPEC-0006 BR8). Idempotent — a repeated
   * accept of an already-recorded version is a no-op. Audited on first record.
   *
   * @throws LegalVersionOutdatedException when {@code version} is not the current one (409).
   */
  @Transactional
  public void accept(
      String accountEmail, String apiType, String version, AuditContext auditContext) {
    UUID accountId = accountId(accountEmail);
    LegalDocument current = currentDocument(apiType);
    if (!current.getVersion().equals(version)) {
      throw new LegalVersionOutdatedException();
    }
    String acceptanceCode = LegalDocumentTypes.acceptanceCodeFor(apiType);
    if (acceptances.existsByAccountIdAndDocumentTypeAndVersion(
        accountId, acceptanceCode, version)) {
      return;
    }
    acceptances.save(TermAcceptance.record(accountId, acceptanceCode, version, clock.instant()));
    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.TERM_ACCEPTED,
            accountId,
            null,
            Map.of("type", apiType, "version", version),
            auditContext));
  }

  private LegalDocumentStatus statusFor(String apiType, UUID accountId) {
    LegalDocument current = currentDocument(apiType);
    return new LegalDocumentStatus(
        current.getVersion(),
        current.getPublishedAt(),
        isAccepted(accountId, apiType, current.getVersion()));
  }

  private boolean isAccepted(UUID accountId, String apiType, String version) {
    return acceptances.existsByAccountIdAndDocumentTypeAndVersion(
        accountId, LegalDocumentTypes.acceptanceCodeFor(apiType), version);
  }

  private LegalDocument currentDocument(String apiType) {
    return documents
        .findFirstByTypeOrderByPublishedAtDesc(apiType)
        .orElseThrow(
            () -> new IllegalStateException("no current legal document seeded for " + apiType));
  }

  private UUID accountId(String email) {
    return accounts
        .findByEmail(Emails.normalize(email))
        .map(UserAccount::getId)
        .orElseThrow(
            () -> new IllegalStateException("authenticated user has no account: " + email));
  }
}
