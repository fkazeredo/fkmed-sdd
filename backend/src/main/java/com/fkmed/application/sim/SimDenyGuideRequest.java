package com.fkmed.application.sim;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /api/sim/guides/{id}/deny} (SPEC-0018 BR5). */
public record SimDenyGuideRequest(@NotBlank String reason) {}
