package com.fkmed.application.api.dto;

import com.fkmed.domain.reimbursement.DocumentCategory;
import jakarta.validation.constraints.NotNull;

/**
 * Metadata for one uploaded document. The multipart file travels in the same order in the {@code
 * documents} parts.
 */
public record ReimbursementDocumentRequest(@NotNull DocumentCategory category, String fileName) {}
