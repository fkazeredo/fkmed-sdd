package com.fkmed.application.api.dto;

/** Payload of {@code GET /api/system/health} (SPEC-0001 BR1): application and database status. */
public record SystemHealthResponse(String status, String database) {

  public static SystemHealthResponse of(boolean databaseUp) {
    return new SystemHealthResponse(databaseUp ? "UP" : "DOWN", databaseUp ? "UP" : "DOWN");
  }
}
