package com.fkmed.application.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Body of {@code POST /api/support/libras-requests} (SPEC-0014 BR4): the requesting beneficiary.
 */
public record LibrasRequestInput(@NotNull UUID beneficiaryId) {}
