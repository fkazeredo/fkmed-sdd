package com.fkmed.application.sim;

import java.util.UUID;

/** The id of a document issued by {@code POST /api/sim/documents} (SPEC-0018 BR5). */
public record SimIssuedDocumentResponse(UUID documentId) {}
