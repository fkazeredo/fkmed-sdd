package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fkmed.application.api.TeleSessionStream;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0010 API contract over MockMvc + Testcontainers Postgres: the triage catalog, entering the
 * queue (201 with position/ETA), the triage/term validations, the single-session resume (200), the
 * live current-session read as plain JSON and as an SSE stream, and leaving the queue. The caller
 * card is resolved server-side from the JWT (ADR-0009); tele tables are cleaned in
 * {@code @BeforeEach} (Postgres is a shared singleton, and queue position asserts absolute counts).
 */
class TeleApiIT extends AbstractIntegrationTest {

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final String MARIA_ACCOUNT_ID = "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70";
  private static final String PEDRO_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TeleSessionStream stream;

  @BeforeEach
  void setUp() {
    jdbc.update("delete from tele_session_symptom");
    jdbc.update("delete from tele_session");
  }

  @Test
  void catalog_returnsSymptomsAndCurrentTerm() throws Exception {
    mockMvc
        .perform(get("/api/tele/catalog").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.symptoms[?(@.code=='CEFALEIA')].name")
                .value(Matchers.hasItem("Dor de cabeça")))
        .andExpect(jsonPath("$.term.version").value("1.0"))
        .andExpect(jsonPath("$.term.body").isNotEmpty());
  }

  @Test
  void enter_returns201_emFila_position1_eta3_recordsAuthorAndBeneficiary() throws Exception {
    mockMvc
        .perform(enter(MARIA_ID, "Dor de cabeça há dois dias", "\"CEFALEIA\"", "D1_3", "1.0"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.state").value("EM_FILA"))
        .andExpect(jsonPath("$.position").value(1))
        .andExpect(jsonPath("$.etaMinutes").value(3));

    assertThat(sessionCountFor(MARIA_ID)).isEqualTo(1);
    assertThat(createdBy(MARIA_ID)).isEqualTo(MARIA_ACCOUNT_ID);
  }

  @Test
  void enterForDependentAsTitular_bindsToDependent_recordsTitularAsAuthor() throws Exception {
    mockMvc
        .perform(enter(PEDRO_ID, "Febre há um dia", "\"FEBRE\"", "HORAS", "1.0"))
        .andExpect(status().isCreated());

    assertThat(sessionCountFor(PEDRO_ID)).isEqualTo(1);
    assertThat(createdBy(PEDRO_ID)).isEqualTo(MARIA_ACCOUNT_ID);
  }

  @Test
  void position_growsWithTheQueueAhead() throws Exception {
    mockMvc.perform(enter(PEDRO_ID, "Febre há um dia", "\"FEBRE\"", "HORAS", "1.0"));
    // MARIA enters after PEDRO, so she is second in line with a 6-minute estimate.
    mockMvc
        .perform(enter(MARIA_ID, "Dor de cabeça há dois dias", "\"CEFALEIA\"", "D1_3", "1.0"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.position").value(2))
        .andExpect(jsonPath("$.etaMinutes").value(6));
  }

  @Test
  void enterWithShortComplaint_returns422_complaintInvalid() throws Exception {
    mockMvc
        .perform(enter(MARIA_ID, "curto", "\"CEFALEIA\"", "D1_3", "1.0"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("tele.complaint-invalid"));

    assertThat(sessionCountFor(MARIA_ID)).isZero();
  }

  @Test
  void enterWithStaleTermVersion_returns422_termNotAccepted() throws Exception {
    mockMvc
        .perform(enter(MARIA_ID, "Dor de cabeça há dois dias", "\"CEFALEIA\"", "D1_3", "0.9"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("tele.term-not-accepted"));
  }

  @Test
  void enterWithUnknownSymptomOrDuration_returns422_triageInvalid() throws Exception {
    mockMvc
        .perform(enter(MARIA_ID, "Dor de cabeça há dois dias", "\"NAO_EXISTE\"", "D1_3", "1.0"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("tele.triage-invalid"));

    mockMvc
        .perform(enter(MARIA_ID, "Dor de cabeça há dois dias", "\"CEFALEIA\"", "MESES", "1.0"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("tele.triage-invalid"));
  }

  @Test
  void secondEnterWhileQueued_returns200_resumesTheSameSession_keepingPosition() throws Exception {
    mockMvc
        .perform(enter(MARIA_ID, "Dor de cabeça há dois dias", "\"CEFALEIA\"", "D1_3", "1.0"))
        .andExpect(status().isCreated());
    mockMvc
        .perform(enter(MARIA_ID, "Outra descrição igualmente válida", "\"FEBRE\"", "HORAS", "1.0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("EM_FILA"))
        .andExpect(jsonPath("$.position").value(1));

    assertThat(sessionCountFor(MARIA_ID)).isEqualTo(1);
  }

  @Test
  void currentJson_withoutActiveSession_returns404_sessionNotFound() throws Exception {
    mockMvc
        .perform(
            get("/api/tele/sessions/current")
                .accept(MediaType.APPLICATION_JSON)
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("tele.session-not-found"));
  }

  @Test
  void currentJson_withActiveSession_returnsTheLiveState() throws Exception {
    mockMvc.perform(enter(MARIA_ID, "Dor de cabeça há dois dias", "\"CEFALEIA\"", "D1_3", "1.0"));
    mockMvc
        .perform(
            get("/api/tele/sessions/current")
                .accept(MediaType.APPLICATION_JSON)
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("EM_FILA"))
        .andExpect(jsonPath("$.position").value(1))
        .andExpect(jsonPath("$.etaMinutes").value(3));
  }

  @Test
  void currentAsEventStream_negotiatesSse_andPushesTheState() throws Exception {
    mockMvc.perform(enter(MARIA_ID, "Dor de cabeça há dois dias", "\"CEFALEIA\"", "D1_3", "1.0"));

    MvcResult started =
        mockMvc
            .perform(
                get("/api/tele/sessions/current")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .with(authAs(MARIA_EMAIL)))
            .andExpect(request().asyncStarted())
            .andReturn();

    // Complete the long-lived stream so the async result resolves and the buffered event renders.
    stream.complete(currentSessionId(MARIA_ID));

    mockMvc
        .perform(asyncDispatch(started))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
        .andExpect(content().string(Matchers.containsString("EM_FILA")));
  }

  @Test
  void leave_abandonsTheSession_thenCurrentIs404() throws Exception {
    mockMvc.perform(enter(MARIA_ID, "Dor de cabeça há dois dias", "\"CEFALEIA\"", "D1_3", "1.0"));

    mockMvc
        .perform(post("/api/tele/sessions/current/leave").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("ABANDONADA"));

    mockMvc
        .perform(
            get("/api/tele/sessions/current")
                .accept(MediaType.APPLICATION_JSON)
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound());
  }

  @Test
  void leave_withoutActiveSession_returns404() throws Exception {
    mockMvc
        .perform(post("/api/tele/sessions/current/leave").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("tele.session-not-found"));
  }

  @Test
  void enter_withoutAuthentication_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/tele/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(MARIA_ID, "Dor de cabeça há dois dias", "\"CEFALEIA\"", "D1_3", "1.0")))
        .andExpect(status().isUnauthorized());
  }

  // --- helpers ---

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder enter(
      String beneficiaryId, String complaint, String symptom, String duration, String term) {
    return post("/api/tele/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body(beneficiaryId, complaint, symptom, duration, term))
        .with(authAs(MARIA_EMAIL));
  }

  private static String body(
      String beneficiaryId, String complaint, String symptom, String duration, String term) {
    return "{\"beneficiaryId\":\""
        + beneficiaryId
        + "\",\"complaint\":\""
        + complaint
        + "\",\"symptoms\":["
        + symptom
        + "],\"duration\":\""
        + duration
        + "\",\"termVersion\":\""
        + term
        + "\"}";
  }

  private int sessionCountFor(String beneficiaryId) {
    return jdbc.queryForObject(
        "select count(*) from tele_session where beneficiary_id = ?::uuid",
        Integer.class,
        beneficiaryId);
  }

  private String createdBy(String beneficiaryId) {
    return jdbc.queryForObject(
        "select created_by from tele_session where beneficiary_id = ?::uuid",
        String.class,
        beneficiaryId);
  }

  private UUID currentSessionId(String beneficiaryId) {
    return jdbc.queryForObject(
        "select id from tele_session where beneficiary_id = ?::uuid and state = 'EM_FILA'",
        UUID.class,
        beneficiaryId);
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
