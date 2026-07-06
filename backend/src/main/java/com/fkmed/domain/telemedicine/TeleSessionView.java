package com.fkmed.domain.telemedicine;

import java.time.Instant;
import java.util.List;

/**
 * The live view of the caller's current session (SPEC-0010 GET /api/tele/sessions/current, JSON and
 * SSE payload). {@code position}/{@code etaMinutes} are present only while queued ({@code EM_FILA}
 * walk-in); {@code professional} and {@code room} are present once being attended ({@code
 * EM_ATENDIMENTO}) and at closure ({@code ENCERRADA}).
 *
 * <p>The {@code room} shape mirrors the merged frontend's {@code TeleRoom} (Phase-4 Wave-2
 * reconciliation): {@code startedAt} drives the running duration while attended (BR9); at closure
 * it also carries the summary — {@code durationMinutes}, {@code guidance} and the issued {@code
 * documents} the "Ver em Minha Saúde" list renders (BR9/BR10).
 */
public record TeleSessionView(
    String state, Integer position, Integer etaMinutes, Professional professional, Room room) {

  /** The attending professional (name + CRM), shown in the room (BR9). */
  public record Professional(String name, String crm) {}

  /**
   * The room/closure-summary container (BR9). {@code startedAt} is the participation start (running
   * duration source); {@code durationMinutes}/{@code guidance}/{@code documents} are populated at
   * closure. Null fields are simply absent for the client.
   */
  public record Room(
      Instant startedAt,
      Integer durationMinutes,
      String guidance,
      List<IssuedDocument> documents) {}

  /** A document issued at closure (BR9/BR10), opened in Minha Saúde (SPEC-0011). */
  public record IssuedDocument(String id, String type) {}
}
