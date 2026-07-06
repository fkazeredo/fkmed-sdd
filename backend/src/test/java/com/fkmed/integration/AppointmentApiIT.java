package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0009 API contract (all endpoints + error codes) over MockMvc and Testcontainers Postgres:
 * booking (consultation JSON + exam multipart), the slot-taken/time-conflict/attachment/horizon
 * errors, cancel (seat release) and reschedule (protocol kept), REALIZADO derivation, family scope
 * (book for a dependent as the titular, author recorded) and Meus Agendamentos. The caller's card
 * is resolved server-side from the JWT subject (ADR-0009); tests build a capacity-controlled
 * fixture rather than the capacity-5 seed and clean in {@code @BeforeEach} (Postgres is a shared
 * singleton).
 */
class AppointmentApiIT extends AbstractIntegrationTest {

  private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final String MARIA_ACCOUNT_ID = "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70";
  private static final String PEDRO_EMAIL = PedroAccountFixture.PEDRO_EMAIL;
  private static final String PEDRO_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";

  private static final String UNIT = "cc000000-0000-4000-8000-000000000001";
  private static final String AG_CONSULT = "cc000000-0000-4000-8000-000000000010";
  private static final String AG_EXAM = "cc000000-0000-4000-8000-000000000011";
  private static final String SLOT_A = "cc000000-0000-4000-8000-0000000000a1";
  private static final String SLOT_B = "cc000000-0000-4000-8000-0000000000a2";
  private static final String SLOT_EXAM = "cc000000-0000-4000-8000-0000000000a3";
  private static final String SLOT_FAR = "cc000000-0000-4000-8000-0000000000a4";

  private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31}; // %PDF-1

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  private LocalDate d1;

  @BeforeEach
  void setUp() {
    clean();
    PedroAccountFixture.seed(jdbc);

    d1 = LocalDate.now(CLINIC_ZONE).plusDays(3);
    LocalDate far = LocalDate.now(CLINIC_ZONE).plusDays(40);

    jdbc.update(
        "insert into care_unit (id, name, neighborhood, city, uf, phone)"
            + " values (?::uuid, 'Unidade API', 'Centro', 'Rio de Janeiro', 'RJ', '(21) 3333-8000')",
        UNIT);
    jdbc.update(
        "insert into unit_agenda (id, unit_id, scope_type, scope_code)"
            + " values (?::uuid, ?::uuid, 'CONSULTATION', 'CARDIOLOGIA')",
        AG_CONSULT,
        UNIT);
    jdbc.update(
        "insert into unit_agenda (id, unit_id, scope_type, scope_code)"
            + " values (?::uuid, ?::uuid, 'EXAM', 'HEMOGRAMA')",
        AG_EXAM,
        UNIT);
    insertSlot(SLOT_A, AG_CONSULT, d1, LocalTime.of(9, 0), 2, 0);
    insertSlot(SLOT_B, AG_CONSULT, d1, LocalTime.of(10, 0), 2, 0);
    insertSlot(SLOT_EXAM, AG_EXAM, d1, LocalTime.of(9, 0), 2, 0);
    insertSlot(SLOT_FAR, AG_CONSULT, far, LocalTime.of(9, 0), 2, 0);
  }

  @AfterEach
  void tearDown() {
    PedroAccountFixture.remove(jdbc);
  }

  @Test
  void units_forCardiology_includeTheSeededOwnUnits() throws Exception {
    mockMvc
        .perform(
            get("/api/appointments/units")
                .param("specialty", "CARDIOLOGIA")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name=='FKMed Unidade Centro')]").exists())
        .andExpect(jsonPath("$[?(@.name=='FKMed Unidade Tijuca')]").exists());
  }

  @Test
  void availability_listsHorizonSlotsWithRemainingCapacity_excludingBeyondHorizon()
      throws Exception {
    mockMvc
        .perform(
            get("/api/appointments/availability")
                .param("unitId", UNIT)
                .param("specialty", "CARDIOLOGIA")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days.length()").value(1))
        .andExpect(jsonPath("$.days[0].date").value(d1.toString()))
        .andExpect(jsonPath("$.days[0].slots.length()").value(2))
        .andExpect(jsonPath("$.days[0].slots[0].remaining").value(2));
  }

  @Test
  void bookConsultation_returns201_protocolAndAgendado_occupiesSeat_recordsAuthor()
      throws Exception {
    mockMvc
        .perform(
            bookJson(MARIA_ID, "CONSULTATION", "\"specialty\":\"CARDIOLOGIA\",", SLOT_A_TIME()))
        .andExpect(status().isCreated())
        .andExpect(
            jsonPath("$.protocol")
                .value(org.hamcrest.Matchers.matchesPattern("^AG-\\d{8}-\\d{4}$")))
        .andExpect(jsonPath("$.status").value("AGENDADO"));

    assertThat(occupied(SLOT_A)).isEqualTo(1);
    assertThat(createdBy(MARIA_ID, SLOT_A)).isEqualTo(MARIA_ACCOUNT_ID);
  }

  @Test
  void bookForDependentAsTitular_bindsToDependent_recordsTitularAsAuthor() throws Exception {
    mockMvc
        .perform(
            bookJson(PEDRO_ID, "CONSULTATION", "\"specialty\":\"CARDIOLOGIA\",", SLOT_A_TIME()))
        .andExpect(status().isCreated());

    assertThat(occupied(SLOT_A)).isEqualTo(1);
    assertThat(createdBy(PEDRO_ID, SLOT_A)).isEqualTo(MARIA_ACCOUNT_ID);
  }

  @Test
  void bookExamWithoutAttachment_returns422_attachmentRequired_withoutOccupying() throws Exception {
    mockMvc
        .perform(bookJson(MARIA_ID, "EXAM", "\"exam\":\"HEMOGRAMA\",", d1 + "T09:00"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("appointment.attachment-required"));

    assertThat(occupied(SLOT_EXAM)).isZero();
  }

  @Test
  void bookExamWithInvalidAttachment_returns422_attachmentInvalid() throws Exception {
    mockMvc
        .perform(examMultipart("hello".getBytes(), d1 + "T09:00").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("appointment.attachment-invalid"));

    assertThat(occupied(SLOT_EXAM)).isZero();
  }

  @Test
  void bookExamWithValidPdf_returns201_andStoresTheAttachment() throws Exception {
    mockMvc
        .perform(examMultipart(PDF, d1 + "T09:00").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("AGENDADO"));

    assertThat(occupied(SLOT_EXAM)).isEqualTo(1);
    String appointmentId =
        jdbc.queryForObject(
            "select id from appointment where beneficiary_id=?::uuid and slot_id=?::uuid",
            String.class,
            MARIA_ID,
            SLOT_EXAM);
    Long attachments =
        jdbc.queryForObject(
            "select count(*) from appointment_attachment where appointment_id=?::uuid",
            Long.class,
            appointmentId);
    assertThat(attachments).isEqualTo(1);
  }

  @Test
  void bookFullSlot_returns409_slotTaken() throws Exception {
    jdbc.update("update schedule_slot set occupied = capacity where id = ?::uuid", SLOT_A);

    mockMvc
        .perform(
            bookJson(MARIA_ID, "CONSULTATION", "\"specialty\":\"CARDIOLOGIA\",", SLOT_A_TIME()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("appointment.slot-taken"));
  }

  @Test
  void secondBookingSameBeneficiarySameInstant_returns409_timeConflict() throws Exception {
    mockMvc
        .perform(
            bookJson(MARIA_ID, "CONSULTATION", "\"specialty\":\"CARDIOLOGIA\",", SLOT_A_TIME()))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            bookJson(MARIA_ID, "CONSULTATION", "\"specialty\":\"CARDIOLOGIA\",", SLOT_A_TIME()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("appointment.time-conflict"));
  }

  @Test
  void bookBeyondHorizon_returns422_outsideHorizon() throws Exception {
    LocalDate far = LocalDate.now(CLINIC_ZONE).plusDays(40);
    mockMvc
        .perform(
            bookJson(MARIA_ID, "CONSULTATION", "\"specialty\":\"CARDIOLOGIA\",", far + "T09:00"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("appointment.outside-horizon"));
  }

  @Test
  void cancel_releasesSeat_movesToHistoryAsCancelado() throws Exception {
    mockMvc
        .perform(
            bookJson(MARIA_ID, "CONSULTATION", "\"specialty\":\"CARDIOLOGIA\",", SLOT_A_TIME()))
        .andExpect(status().isCreated());
    String id = appointmentId(MARIA_ID, SLOT_A);

    mockMvc
        .perform(
            post("/api/appointments/{id}/cancel", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"mudança de planos\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELADO"));

    assertThat(occupied(SLOT_A)).isZero();
    mockMvc
        .perform(get("/api/appointments").with(authAs(MARIA_EMAIL)))
        .andExpect(jsonPath("$.history[?(@.status=='CANCELADO')]").exists())
        .andExpect(jsonPath("$.upcoming.length()").value(0));
  }

  @Test
  void cancelAfterStart_returns409_cancelTooLate() throws Exception {
    String id = insertPastAppointment("AG-19990101-0001");

    mockMvc
        .perform(post("/api/appointments/{id}/cancel", id).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("appointment.cancel-too-late"));
  }

  @Test
  void reschedule_keepsProtocol_freesOldSeat_setsReagendado() throws Exception {
    String protocol =
        bookAndReadProtocol(
            bookJson(MARIA_ID, "CONSULTATION", "\"specialty\":\"CARDIOLOGIA\",", SLOT_A_TIME()));
    String id = appointmentId(MARIA_ID, SLOT_A);

    mockMvc
        .perform(
            post("/api/appointments/{id}/reschedule", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slot\":\"" + d1 + "T10:00\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.protocol").value(protocol))
        .andExpect(jsonPath("$.status").value("REAGENDADO"));

    assertThat(occupied(SLOT_A)).isZero();
    assertThat(occupied(SLOT_B)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select slot_id from appointment where id=?::uuid", String.class, id))
        .isEqualTo(SLOT_B);
  }

  @Test
  void pastAppointment_appearsInHistoryAsRealizado() throws Exception {
    insertPastAppointment("AG-19990102-0001");

    mockMvc
        .perform(get("/api/appointments").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.history[?(@.status=='REALIZADO')]").exists())
        .andExpect(jsonPath("$.upcoming.length()").value(0));
  }

  @Test
  void list_coversAllAccessibleBeneficiaries_andFiltersByBeneficiary() throws Exception {
    mockMvc
        .perform(
            bookJson(MARIA_ID, "CONSULTATION", "\"specialty\":\"CARDIOLOGIA\",", SLOT_A_TIME()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            bookJson(PEDRO_ID, "CONSULTATION", "\"specialty\":\"CARDIOLOGIA\",", d1 + "T10:00"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/appointments").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.upcoming.length()").value(2));

    mockMvc
        .perform(
            get("/api/appointments").param("beneficiaryId", PEDRO_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.upcoming.length()").value(1))
        .andExpect(jsonPath("$.upcoming[0].beneficiaryId").value(PEDRO_ID));
  }

  @Test
  void bookForOutOfScopeBeneficiary_returns404_withoutRevealingExistence() throws Exception {
    mockMvc
        .perform(
            bookJson(
                MARIA_ID,
                "CONSULTATION",
                "\"specialty\":\"CARDIOLOGIA\",",
                SLOT_A_TIME(),
                PEDRO_EMAIL))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"));
  }

  @Test
  void cancelUnknownAppointment_returns404() throws Exception {
    mockMvc
        .perform(
            post("/api/appointments/{id}/cancel", "00000000-0000-0000-0000-000000000000")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("appointment.not-found"));
  }

  @Test
  void book_withoutAuthentication_returns401() throws Exception {
    mockMvc
        .perform(
            bookJsonNoAuth(
                MARIA_ID, "CONSULTATION", "\"specialty\":\"CARDIOLOGIA\",", SLOT_A_TIME()))
        .andExpect(status().isUnauthorized());
  }

  // --- helpers ---

  private String SLOT_A_TIME() {
    return d1 + "T09:00";
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder bookJson(
      String beneficiaryId, String type, String scopeField, String slot) {
    return bookJson(beneficiaryId, type, scopeField, slot, MARIA_EMAIL);
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder bookJson(
      String beneficiaryId, String type, String scopeField, String slot, String email) {
    return post("/api/appointments")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body(beneficiaryId, type, scopeField, slot))
        .with(authAs(email));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder bookJsonNoAuth(
      String beneficiaryId, String type, String scopeField, String slot) {
    return post("/api/appointments")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body(beneficiaryId, type, scopeField, slot));
  }

  private static String body(String beneficiaryId, String type, String scopeField, String slot) {
    return "{\"beneficiaryId\":\""
        + beneficiaryId
        + "\",\"type\":\""
        + type
        + "\","
        + scopeField
        + "\"unitId\":\""
        + UNIT
        + "\",\"slot\":\""
        + slot
        + "\"}";
  }

  private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
      examMultipart(byte[] file, String slot) {
    var builder =
        multipart("/api/appointments")
            .file(new MockMultipartFile("medicalOrder", "order.pdf", "application/pdf", file));
    builder.param("beneficiaryId", MARIA_ID);
    builder.param("type", "EXAM");
    builder.param("exam", "HEMOGRAMA");
    builder.param("unitId", UNIT);
    builder.param("slot", slot);
    return builder;
  }

  private String bookAndReadProtocol(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
      throws Exception {
    String json =
        mockMvc
            .perform(request)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.replaceAll(".*\"protocol\"\\s*:\\s*\"([^\"]+)\".*", "$1");
  }

  private String appointmentId(String beneficiaryId, String slotId) {
    return jdbc.queryForObject(
        "select id from appointment where beneficiary_id=?::uuid and slot_id=?::uuid",
        String.class,
        beneficiaryId,
        slotId);
  }

  private int occupied(String slotId) {
    return jdbc.queryForObject(
        "select occupied from schedule_slot where id=?::uuid", Integer.class, slotId);
  }

  private String createdBy(String beneficiaryId, String slotId) {
    return jdbc.queryForObject(
        "select created_by from appointment where beneficiary_id=?::uuid and slot_id=?::uuid",
        String.class,
        beneficiaryId,
        slotId);
  }

  private String insertPastAppointment(String protocol) {
    String id = java.util.UUID.randomUUID().toString();
    Instant past = Instant.now().minus(2, ChronoUnit.DAYS);
    jdbc.update(
        "insert into appointment (id, protocol, type, beneficiary_id, specialty_code, unit_id,"
            + " slot_id, scheduled_at, status, created_by, created_at, updated_at)"
            + " values (?::uuid, ?, 'CONSULTATION', ?::uuid, 'CARDIOLOGIA', ?::uuid, ?::uuid, ?,"
            + " 'AGENDADO', ?::uuid, ?, ?)",
        id,
        protocol,
        MARIA_ID,
        UNIT,
        SLOT_A,
        java.sql.Timestamp.from(past),
        MARIA_ACCOUNT_ID,
        java.sql.Timestamp.from(past),
        java.sql.Timestamp.from(past));
    return id;
  }

  private void insertSlot(
      String id, String agendaId, LocalDate date, LocalTime time, int capacity, int occupied) {
    jdbc.update(
        "insert into schedule_slot (id, agenda_id, slot_date, slot_time, capacity, occupied, version)"
            + " values (?::uuid, ?::uuid, ?, ?, ?, ?, 0)",
        id,
        agendaId,
        date,
        time,
        capacity,
        occupied);
  }

  private void clean() {
    jdbc.update(
        "delete from appointment_attachment where appointment_id in"
            + " (select id from appointment where beneficiary_id in (?::uuid, ?::uuid))",
        MARIA_ID,
        PEDRO_ID);
    jdbc.update(
        "delete from appointment where beneficiary_id in (?::uuid, ?::uuid)", MARIA_ID, PEDRO_ID);
    jdbc.update(
        "delete from schedule_slot where agenda_id in (?::uuid, ?::uuid)", AG_CONSULT, AG_EXAM);
    jdbc.update("delete from unit_agenda where unit_id = ?::uuid", UNIT);
    jdbc.update("delete from care_unit where id = ?::uuid", UNIT);
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
