package com.fkmed.application.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Create-preview metadata. Consulta ignores documents; analyzed types require them. */
public record ReimbursementPreviewRequest(
    @NotNull UUID beneficiaryId,
    @NotBlank String expenseTypeCode,
    List<@Valid ReimbursementDocumentRequest> documents) {}
