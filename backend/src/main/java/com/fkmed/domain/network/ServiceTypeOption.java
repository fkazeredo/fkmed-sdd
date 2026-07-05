package com.fkmed.domain.network;

/**
 * A service-type registry option for the funnel's first step (SPEC-0008 BR5). {@code
 * hasSpecialtyStep} tells the caller whether picking this type opens the specialty step or skips
 * straight to results (AC3) — data-driven, never hardcoded on a specific code.
 */
public record ServiceTypeOption(String code, String name, boolean hasSpecialtyStep) {}
