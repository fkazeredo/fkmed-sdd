package com.fkmed.domain.guides;

import java.time.Instant;

/** The 6-digit attendance code plus its expiry (SPEC-0012 BR9). */
public record TokenView(String code, Instant expiresAt) {}
