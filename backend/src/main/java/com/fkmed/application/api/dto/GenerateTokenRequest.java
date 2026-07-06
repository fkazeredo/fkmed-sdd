package com.fkmed.application.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Body of {@code POST /api/tokens} (SPEC-0012 BR9): the beneficiary the token is generated for. */
public record GenerateTokenRequest(@NotNull UUID beneficiaryId) {}
