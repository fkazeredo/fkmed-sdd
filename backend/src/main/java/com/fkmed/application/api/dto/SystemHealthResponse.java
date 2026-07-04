package com.fkmed.application.api.dto;

/**
 * Payload of {@code GET /api/system/health} (SPEC-0001 BR1): {@code status} is the application (UP
 * whenever it answers); {@code database} reflects the connectivity probe.
 */
public record SystemHealthResponse(String status, String database) {

  public static SystemHealthResponse of(boolean databaseUp) {
    return new SystemHealthResponse("UP", databaseUp ? "UP" : "DOWN");
  }
}
