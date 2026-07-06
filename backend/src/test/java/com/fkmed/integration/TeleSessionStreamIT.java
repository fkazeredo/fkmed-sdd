package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.application.api.TeleSessionStream;
import com.fkmed.domain.telemedicine.EnterQueueCommand;
import com.fkmed.domain.telemedicine.TeleService;
import com.fkmed.domain.telemedicine.TeleSessionView;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SPEC-0010 / ADR-0016 SSE emitter lifecycle: opening a stream registers it and pushes the initial
 * state; a periodic re-emit keeps an active session's stream open; once the session reaches a final
 * state the re-emit completes and deregisters the stream (no leaks). Exercised against the real
 * {@link TeleSessionStream} registry and {@link TeleService} over Testcontainers Postgres.
 */
class TeleSessionStreamIT extends AbstractIntegrationTest {

  private static final String MARIA_CARD = "001234567";
  private static final UUID MARIA_ID = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");

  @Autowired private TeleService tele;
  @Autowired private TeleSessionStream stream;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    jdbc.update("delete from tele_session_symptom");
    jdbc.update("delete from tele_session");
  }

  @Test
  void open_registersAndPushes_thenReEmitCompletesWhenTheSessionGoesFinal() {
    UUID sessionId = enterQueue();
    TeleSessionView initial = tele.viewOf(sessionId).orElseThrow();

    SseEmitter emitter = stream.open(sessionId, initial);
    assertThat(emitter).isNotNull();
    assertThat(stream.openSessionIds()).as("the stream is registered on open").contains(sessionId);

    // Active session: a re-emit succeeds and keeps the stream open (a failed send would drop it).
    stream.broadcast();
    assertThat(stream.openSessionIds()).contains(sessionId);

    // Session goes final (leaves the queue): the next re-emit completes and deregisters the stream.
    tele.leave(MARIA_CARD);
    stream.broadcast();

    assertThat(stream.openSessionIds())
        .as("the stream is completed and deregistered once the session is final")
        .doesNotContain(sessionId);
  }

  private UUID enterQueue() {
    tele.enterQueue(
        new EnterQueueCommand(
            MARIA_CARD,
            null,
            MARIA_ID,
            "Dor de cabeça há dois dias",
            List.of("CEFALEIA"),
            null,
            "D1_3",
            "1.0"));
    return jdbc.queryForObject(
        "select id from tele_session where beneficiary_id = ?::uuid and state = 'EM_FILA'",
        UUID.class,
        MARIA_ID);
  }
}
