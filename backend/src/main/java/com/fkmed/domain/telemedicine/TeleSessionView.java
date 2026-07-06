package com.fkmed.domain.telemedicine;

/**
 * The live view of the caller's current session (SPEC-0010 GET /api/tele/sessions/current, JSON and
 * SSE payload). {@code position}/{@code etaMinutes} are present only while queued ({@code EM_FILA}
 * walk-in); {@code professional}/{@code room} are present only once being attended ({@code
 * EM_ATENDIMENTO}).
 */
public record TeleSessionView(
    String state, Integer position, Integer etaMinutes, Professional professional, String room) {

  /** The attending professional (name + CRM), shown in the room (BR9). */
  public record Professional(String name, String crm) {}
}
