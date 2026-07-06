package com.fkmed.application.sim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Body of {@code POST /api/sim/guides/{id}/authorize} (SPEC-0018 BR5). */
public record SimAuthorizeGuideRequest(@NotBlank String password, @NotNull LocalDate validUntil) {}
