package com.fkmed.application.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/legal-documents/{type}/accept} (SPEC-0006 BR8): the version the user is
 * accepting — the one shown on screen. The server records it only when it is still the current
 * version, else answers 409 {@code legal.version-outdated} (a newer version was published
 * meanwhile).
 */
public record AcceptLegalDocumentRequest(@NotBlank String version) {}
