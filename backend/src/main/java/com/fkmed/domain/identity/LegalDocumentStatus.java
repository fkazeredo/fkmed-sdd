package com.fkmed.domain.identity;

import java.time.Instant;

/**
 * A legal document's current version metadata plus whether the caller has accepted it (SPEC-0006
 * §API Contracts — {@code GET /api/legal-documents/current}). Drives the frontend re-acceptance
 * interception; carries no body (the pages fetch it separately).
 */
public record LegalDocumentStatus(String version, Instant publishedAt, boolean acceptedByMe) {}
