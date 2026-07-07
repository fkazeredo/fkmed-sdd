package com.fkmed.application.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Body of {@code POST /api/support/libras-requests} (SPEC-0014 BR4): the beneficiary the request is
 * for, scope-checked server-side against the caller's family (SPEC-0003 BR3).
 */
public record LibrasServiceRequest(@NotNull UUID beneficiaryId) {}
