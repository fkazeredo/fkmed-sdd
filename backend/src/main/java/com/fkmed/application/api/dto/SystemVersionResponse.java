package com.fkmed.application.api.dto;

/** Payload of {@code GET /api/system/version} (SPEC-0001 BR2): build version and git commit. */
public record SystemVersionResponse(String version, String commit) {}
