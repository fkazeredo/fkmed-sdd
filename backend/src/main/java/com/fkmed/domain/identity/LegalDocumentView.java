package com.fkmed.domain.identity;

import java.time.Instant;

/**
 * A full legal-document version for its page (SPEC-0006 BR8): version, publication date and the
 * body text, plus whether the caller has already accepted this current version.
 */
public record LegalDocumentView(
    String type, String version, Instant publishedAt, String body, boolean acceptedByMe) {}
