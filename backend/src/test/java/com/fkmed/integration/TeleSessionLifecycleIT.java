package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fkmed.domain.telemedicine.EnterQueueCommand;
import com.fkmed.domain.telemedicine.EnterQueueResult;
import com.fkmed.domain.telemedicine.TeleClosureSummary;
import com.fkmed.domain.telemedicine.TeleService;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0010 session lifecycle over Testcontainers Postgres: the professional-side seams the
 * operator simulation drives ({@code reachTurn}/{@code close}), the 5-minute no-show expiry
 * (BR8/AC3) with a responded control, and the scheduled-teleconsultation join window
 * (BR14/DL-0018). Tele tables are cleaned in {@code @BeforeEach} (shared Postgres singleton).
 */
class TeleSessionLifecycleIT extends AbstractIntegrationTest {

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_CARD = "001234567";
  private static final UUID MARIA_ID = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");
  private static final UUID MARIA_ACCOUNT_ID =
      UUID.fromString("d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70");
  private static final UUID PEDRO_ID = UUID.fromString("9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");
  private static final UUID TELE_UNIT = UUID.fromString("dd000000-0000-4000-8000-000000000001");

  private static final UUID FIX_SLOT = UUID.fromString("ee000000-0000-4000-8000-000000000001");
  private static final UUID FIX_APPT = UUID.fromString("ee000000-0000-4000-8000-000000000002");

  @Autowired private TeleService tele;
  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    cleanFixtures();
  }

  @AfterEach
  void tearDown() {
    cleanFixtures();
  }

  @Test
  void reachTurn_thenClose_walkTheSessionThroughTheStateMachine() {
    UUID sessionId = enterQueue();

    tele.reachTurn(sessionId, "Dra. Ana", "CRM-RJ 12345");
    assertThat(state(sessionId)).isEqualTo("EM_ATENDIMENTO");
    assertThat(
            jdbc.queryForObject(
                "select professional_name from tele_session where id = ?::uuid",
                String.class,
                sessionId))
        .isEqualTo("Dra. Ana");

    tele.close(
        sessionId, new TeleClosureSummary("Dra. Ana", "CRM-RJ 12345", "Repouso e hidratação"));
    assertThat(state(sessionId)).isEqualTo("ENCERRADA");
    assertThat(
            jdbc.queryForObject(
                "select guidance from tele_session where id = ?::uuid", String.class, sessionId))
        .isEqualTo("Repouso e hidratação");
  }

  @Test
  void noShow_expiresAnAttendedSessionAfterFiveMinutesWithoutResponse() {
    Instant now = Instant.now();
    // Two attended sessions on distinct beneficiaries (the single-active-session index is per
    // beneficiary): one never responded, the other did.
    UUID stale = insertAttended(MARIA_ID, now.minus(Duration.ofMinutes(6)), null);
    UUID responded =
        insertAttended(
            PEDRO_ID, now.minus(Duration.ofMinutes(6)), now.minus(Duration.ofMinutes(5)));

    tele.expireNoShows();

    assertThat(state(stale)).as("no response within 5 min → abandoned").isEqualTo("ABANDONADA");
    assertThat(state(responded)).as("responded → still attending").isEqualTo("EM_ATENDIMENTO");
  }

  @Test
  void joinScheduled_withinWindow_opensTheRoom_thenResumesOnReJoin() throws Exception {
    seedTeleAppointment(Instant.now());

    mockMvc
        .perform(post("/api/appointments/{id}/join", FIX_APPT).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("EM_FILA"));

    // Re-join resumes the same scheduled session (no duplicate).
    mockMvc
        .perform(post("/api/appointments/{id}/join", FIX_APPT).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk());

    assertThat(
            jdbc.queryForObject(
                "select count(*) from tele_session where appointment_id = ?::uuid",
                Long.class,
                FIX_APPT))
        .isEqualTo(1);
  }

  @Test
  void joinScheduled_beforeTheWindowOpens_returns409_joinWindowClosed() throws Exception {
    // A slot 30 minutes away: the window opens only 10 minutes before, so now is too early.
    seedTeleAppointment(Instant.now().plus(Duration.ofMinutes(30)));

    mockMvc
        .perform(post("/api/appointments/{id}/join", FIX_APPT).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("tele.join-window-closed"));
  }

  @Test
  void joinScheduled_forAPresencialAppointment_returns404() throws Exception {
    seedAppointment(Instant.now(), "PRESENCIAL");

    mockMvc
        .perform(post("/api/appointments/{id}/join", FIX_APPT).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("tele.session-not-found"));
  }

  // --- helpers ---

  private UUID enterQueue() {
    EnterQueueResult ignored =
        tele.enterQueue(
            new EnterQueueCommand(
                MARIA_CARD,
                MARIA_ACCOUNT_ID,
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

  private UUID insertAttended(UUID beneficiaryId, Instant calledAt, Instant startedAt) {
    UUID id = UUID.randomUUID();
    Instant queued = calledAt.minus(Duration.ofMinutes(1));
    jdbc.update(
        "insert into tele_session (id, beneficiary_id, type, state, complaint, duration_code,"
            + " term_version, professional_name, professional_crm, queue_entered_at, called_at,"
            + " started_at, created_by, created_at, updated_at, version)"
            + " values (?::uuid, ?::uuid, 'WALK_IN', 'EM_ATENDIMENTO', 'Dor de cabeça há dois dias',"
            + " 'D1_3', '1.0', 'Dra. Ana', 'CRM-RJ 1', ?, ?, ?, ?::uuid, ?, ?, 0)",
        id,
        beneficiaryId,
        Timestamp.from(queued),
        Timestamp.from(calledAt),
        startedAt == null ? null : Timestamp.from(startedAt),
        MARIA_ACCOUNT_ID,
        Timestamp.from(queued),
        Timestamp.from(calledAt));
    return id;
  }

  private void seedTeleAppointment(Instant scheduledAt) {
    seedAppointment(scheduledAt, "TELEMEDICINA");
  }

  private void seedAppointment(Instant scheduledAt, String modality) {
    UUID agendaId =
        jdbc.queryForObject(
            "select id from unit_agenda where unit_id = ?::uuid limit 1", UUID.class, TELE_UNIT);
    // A non-seeded slot time (the V19 tele agenda seeds :00/:30 slots) so this fixture never
    // collides with the seeded capacity; the join window is driven by the appointment's
    // scheduled_at,
    // not by the slot time.
    jdbc.update(
        "insert into schedule_slot (id, agenda_id, slot_date, slot_time, capacity, occupied, version)"
            + " values (?::uuid, ?::uuid, current_date, time '09:07', 3, 1, 0)",
        FIX_SLOT,
        agendaId);
    jdbc.update(
        "insert into appointment (id, protocol, type, beneficiary_id, specialty_code, unit_id,"
            + " slot_id, scheduled_at, status, modality, created_by, created_at, updated_at)"
            + " values (?::uuid, 'AG-20260706-9001', 'CONSULTATION', ?::uuid, 'CLINICA_MEDICA',"
            + " ?::uuid, ?::uuid, ?, 'AGENDADO', ?, ?::uuid, now(), now())",
        FIX_APPT,
        MARIA_ID,
        TELE_UNIT,
        FIX_SLOT,
        Timestamp.from(scheduledAt),
        modality,
        MARIA_ACCOUNT_ID);
  }

  private String state(UUID sessionId) {
    return jdbc.queryForObject(
        "select state from tele_session where id = ?::uuid", String.class, sessionId);
  }

  private void cleanFixtures() {
    jdbc.update("delete from tele_session_symptom");
    jdbc.update("delete from tele_session");
    jdbc.update("delete from appointment where id = ?::uuid", FIX_APPT);
    jdbc.update("delete from schedule_slot where id = ?::uuid", FIX_SLOT);
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
