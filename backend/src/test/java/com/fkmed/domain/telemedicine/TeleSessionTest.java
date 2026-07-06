package com.fkmed.domain.telemedicine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The {@link TeleSession} entity behaviour (SPEC-0010): the walk-in/scheduled factories, the
 * turn/response/leave/close transitions and the 5-minute no-show rule (BR8/AC3), all against a
 * fixed clock so the timing is deterministic.
 */
class TeleSessionTest {

  private static final Instant T0 = Instant.parse("2026-07-06T12:00:00Z");
  private static final UUID BEN = UUID.randomUUID();
  private static final UUID AUTHOR = UUID.randomUUID();

  private TeleSession walkIn() {
    return TeleSession.walkIn(
        BEN, "Dor de cabeça há dois dias", List.of("CEFALEIA"), "leve", "D1_3", "1.0", AUTHOR, T0);
  }

  @Test
  void walkIn_opensInQueueWithTheTriage() {
    TeleSession session = walkIn();
    assertThat(session.getState()).isEqualTo(TeleSessionState.EM_FILA);
    assertThat(session.getType()).isEqualTo(TeleSessionType.WALK_IN);
    assertThat(session.getQueueEnteredAt()).isEqualTo(T0);
    assertThat(session.getComplaint()).isEqualTo("Dor de cabeça há dois dias");
    assertThat(session.getSymptomCodes()).containsExactly("CEFALEIA");
    assertThat(session.getDurationCode()).isEqualTo("D1_3");
    assertThat(session.getTermVersion()).isEqualTo("1.0");
    assertThat(session.getCreatedBy()).isEqualTo(AUTHOR);
    assertThat(session.isActive()).isTrue();
  }

  @Test
  void scheduled_opensInQueueLinkedToItsAppointment_withoutTriage() {
    UUID appointmentId = UUID.randomUUID();
    TeleSession session = TeleSession.scheduled(BEN, appointmentId, AUTHOR, T0);
    assertThat(session.getType()).isEqualTo(TeleSessionType.SCHEDULED);
    assertThat(session.getState()).isEqualTo(TeleSessionState.EM_FILA);
    assertThat(session.getAppointmentId()).isEqualTo(appointmentId);
    assertThat(session.getComplaint()).isNull();
    assertThat(session.getSymptomCodes()).isEmpty();
  }

  @Test
  void reachTurn_movesToAttendingAndRecordsTheProfessional() {
    TeleSession session = walkIn();
    session.reachTurn("Dra. Ana", "CRM-RJ 12345", T0.plusSeconds(60));

    assertThat(session.getState()).isEqualTo(TeleSessionState.EM_ATENDIMENTO);
    assertThat(session.getCalledAt()).isEqualTo(T0.plusSeconds(60));
    assertThat(session.getProfessionalName()).isEqualTo("Dra. Ana");
    assertThat(session.getProfessionalCrm()).isEqualTo("CRM-RJ 12345");
    assertThat(session.getStartedAt()).isNull();
  }

  @Test
  void reachTurn_onAnAlreadyAttendedSession_isRejected() {
    TeleSession session = walkIn();
    session.reachTurn("Dra. Ana", "CRM-RJ 12345", T0);
    assertThatThrownBy(() -> session.reachTurn("Dr. Bo", "CRM-RJ 54321", T0))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void markResponded_startsParticipationOnceThenIsIdempotent() {
    TeleSession session = walkIn();
    session.reachTurn("Dra. Ana", "CRM-RJ 12345", T0);

    assertThat(session.markResponded(T0.plusSeconds(30))).isTrue();
    assertThat(session.getStartedAt()).isEqualTo(T0.plusSeconds(30));
    assertThat(session.markResponded(T0.plusSeconds(90))).isFalse();
    assertThat(session.getStartedAt()).isEqualTo(T0.plusSeconds(30));
  }

  @Test
  void markResponded_whileStillQueued_doesNothing() {
    TeleSession session = walkIn();
    assertThat(session.markResponded(T0)).isFalse();
    assertThat(session.getStartedAt()).isNull();
  }

  @Test
  void isNoShow_onlyAfterFiveMinutesWithoutResponse() {
    TeleSession session = walkIn();
    session.reachTurn("Dra. Ana", "CRM-RJ 12345", T0);

    assertThat(session.isNoShow(T0.plus(Duration.ofMinutes(4)), Duration.ofMinutes(5))).isFalse();
    assertThat(session.isNoShow(T0.plus(Duration.ofMinutes(5)), Duration.ofMinutes(5))).isTrue();
    assertThat(session.isNoShow(T0.plus(Duration.ofMinutes(6)), Duration.ofMinutes(5))).isTrue();
  }

  @Test
  void isNoShow_isFalseWhileQueuedOrOnceResponded() {
    TeleSession queued = walkIn();
    assertThat(queued.isNoShow(T0.plus(Duration.ofHours(1)), Duration.ofMinutes(5))).isFalse();

    TeleSession attended = walkIn();
    attended.reachTurn("Dra. Ana", "CRM-RJ 12345", T0);
    attended.markResponded(T0.plusSeconds(10));
    assertThat(attended.isNoShow(T0.plus(Duration.ofMinutes(10)), Duration.ofMinutes(5))).isFalse();
  }

  @Test
  void leave_abandonsFromTheQueue() {
    TeleSession session = walkIn();
    session.leave(T0.plusSeconds(120));
    assertThat(session.getState()).isEqualTo(TeleSessionState.ABANDONADA);
    assertThat(session.getEndedAt()).isEqualTo(T0.plusSeconds(120));
    assertThat(session.isActive()).isFalse();
  }

  @Test
  void leave_onAFinalSession_isRejected() {
    TeleSession session = walkIn();
    session.leave(T0);
    assertThatThrownBy(() -> session.leave(T0)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void expireAsNoShow_abandonsAnAttendedSession() {
    TeleSession session = walkIn();
    session.reachTurn("Dra. Ana", "CRM-RJ 12345", T0);
    session.expireAsNoShow(T0.plus(Duration.ofMinutes(5)));
    assertThat(session.getState()).isEqualTo(TeleSessionState.ABANDONADA);
    assertThat(session.getEndedAt()).isEqualTo(T0.plus(Duration.ofMinutes(5)));
  }

  @Test
  void close_endsTheSessionWithTheProfessionalSummary() {
    TeleSession session = walkIn();
    session.reachTurn("Dra. Ana", "CRM-RJ 12345", T0);
    session.close(
        "Dra. Ana", "CRM-RJ 12345", "Repouso e hidratação", T0.plus(Duration.ofMinutes(8)));

    assertThat(session.getState()).isEqualTo(TeleSessionState.ENCERRADA);
    assertThat(session.getGuidance()).isEqualTo("Repouso e hidratação");
    assertThat(session.getEndedAt()).isEqualTo(T0.plus(Duration.ofMinutes(8)));
  }

  @Test
  void close_whileStillQueued_isRejected() {
    TeleSession session = walkIn();
    assertThatThrownBy(() -> session.close("Dra. Ana", "CRM-RJ 12345", "x", T0))
        .isInstanceOf(IllegalStateException.class);
  }
}
