package com.fkmed.domain.network;

/**
 * A provider seal as shown on a result card or the provider detail (SPEC-0008 BR7/BR12/BR14):
 * {@code name} and {@code description} (the latter shown on hover/touch in the detail screen).
 */
public record SealBadge(String code, String name, String description) {}
