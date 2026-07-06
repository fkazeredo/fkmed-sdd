package com.fkmed.application.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/finance/invoices/validate} (SPEC-0013 BR4): the digitable line to check,
 * as pasted (formatting is stripped and the 47-digit rule applied server-side).
 */
public record ValidateInvoiceRequest(@NotBlank String line) {}
