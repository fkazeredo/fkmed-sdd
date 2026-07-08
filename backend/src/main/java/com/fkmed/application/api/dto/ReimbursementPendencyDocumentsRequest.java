package com.fkmed.application.api.dto;

import jakarta.validation.Valid;
import java.util.List;

/** Metadata wrapper for pendency-resolution uploads. */
public record ReimbursementPendencyDocumentsRequest(
    @Valid List<ReimbursementDocumentRequest> documents) {}
