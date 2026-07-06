package com.fkmed.application.sim;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/sim/tele/sessions/next/start} (SPEC-0018 BR5): the professional starting
 * to attend the next queued session.
 */
public record StartNextTeleRequest(@NotBlank String professionalName, @NotBlank String crm) {}
