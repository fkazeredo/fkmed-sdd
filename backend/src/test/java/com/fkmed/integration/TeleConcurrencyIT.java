package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.telemedicine.EnterQueueCommand;
import com.fkmed.domain.telemedicine.EnterQueueResult;
import com.fkmed.domain.telemedicine.TeleService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SPEC-0010 BR7 — the single-active-session guarantee under concurrency: when a beneficiary sends
 * two Pronto Atendimento POSTs at the same instant, exactly ONE session is created and the other
 * resumes it (never a duplicate, never an error). Exercised over the real transactional path
 * against Testcontainers Postgres with two live threads racing on the {@code uq_tele_active_walkin}
 * partial unique index (the concurrent-create translated to a resume on a fresh read — DL-0005
 * precedent).
 */
class TeleConcurrencyIT extends AbstractIntegrationTest {

  private static final String MARIA_CARD = "001234567";
  private static final UUID MARIA_ID = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");
  private static final UUID MARIA_ACCOUNT_ID =
      UUID.fromString("d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70");

  @Autowired private TeleService tele;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    jdbc.update("delete from tele_session_symptom");
    jdbc.update("delete from tele_session");
  }

  @Test
  void twoSimultaneousEntries_createExactlyOneSession_theOtherResumesIt() throws Exception {
    CyclicBarrier gate = new CyclicBarrier(2);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Outcome> first = pool.submit(enter(gate));
      Future<Outcome> second = pool.submit(enter(gate));
      Outcome a = first.get();
      Outcome b = second.get();

      assertThat(List.of(a, b))
          .as("both callers succeed, none errors")
          .allSatisfy(outcome -> assertThat(outcome.error).isNull());
      assertThat(
              List.of(a, b).stream().filter(o -> o.result != null && !o.result.resumed()).count())
          .as("exactly one caller created the session; the other resumed")
          .isEqualTo(1);
      assertThat(List.of(a, b))
          .allSatisfy(outcome -> assertThat(outcome.result.state()).isEqualTo("EM_FILA"));
    } finally {
      pool.shutdownNow();
    }

    assertThat(activeWalkInSessions()).as("exactly one active session persisted").isEqualTo(1);
  }

  private Callable<Outcome> enter(CyclicBarrier gate) {
    EnterQueueCommand command =
        new EnterQueueCommand(
            MARIA_CARD,
            MARIA_ACCOUNT_ID,
            MARIA_ID,
            "Dor de cabeça há dois dias",
            List.of("CEFALEIA"),
            null,
            "D1_3",
            "1.0");
    return () -> {
      gate.await();
      try {
        return Outcome.ok(tele.enterQueue(command));
      } catch (RuntimeException e) {
        return Outcome.failed(e);
      }
    };
  }

  private long activeWalkInSessions() {
    return jdbc.queryForObject(
        "select count(*) from tele_session where beneficiary_id = ?::uuid"
            + " and type = 'WALK_IN' and state in ('EM_FILA', 'EM_ATENDIMENTO')",
        Long.class,
        MARIA_ID);
  }

  private record Outcome(EnterQueueResult result, Throwable error) {
    static Outcome ok(EnterQueueResult result) {
      return new Outcome(result, null);
    }

    static Outcome failed(Throwable error) {
      return new Outcome(null, error);
    }
  }
}
