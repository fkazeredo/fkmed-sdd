package com.fkmed.domain.identity;

/**
 * The current Terms of Use and Privacy Notice status for the caller (SPEC-0006 §API Contracts —
 * {@code GET /api/legal-documents/current}); the frontend blocks navigation until both are
 * accepted.
 */
public record LegalDocumentsView(LegalDocumentStatus terms, LegalDocumentStatus privacy) {}
