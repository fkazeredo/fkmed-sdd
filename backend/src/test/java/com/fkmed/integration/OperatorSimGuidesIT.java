package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0018 guide slice over MockMvc + Testcontainers Postgres: role enforcement (403 for a
 * beneficiary — BR2), the state-machine guard (409 on an invalid transition — BR4), not-found
 * (404), the create/authorize/partially-authorize/deny/cancel/mark-executed actions and the
 * cross-spec event → notification wiring (SPEC-0012 BR8) — proving a sim-driven authorization is
 * indistinguishable from a real back-office one (SPEC-0018 AC1 / SPEC-0012 AC7). The seeded
 * EM_ANALISE guide used by {@link #authorize_theSeededGuide_notifiesAndShowsThePassword_ac7} is
 * restored to its original state in {@code @BeforeEach}/{@code @AfterEach} (shared Postgres); every
 * other scenario drives a freshly sim-created guide so it never touches the V23 seed.
 */
@Import(RecordingMailConfig.class)
class OperatorSimGuidesIT extends AbstractIntegrationTest {

  private static final String OPERATOR_EMAIL = "operador-sim@fkmed.local";
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final UUID MARIA_ID = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");
  private static final UUID MARIA_ACCOUNT_ID =
      UUID.fromString("d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70");
  private static final UUID OPERATOR_BEN = UUID.fromString("c0000000-0000-4000-8000-000000000001");
  private static final UUID OPERATOR_ACC = UUID.fromString("c0000000-0000-4000-8000-000000000002");
  private static final UUID PLAN_ID = UUID.fromString("b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a");

  private static final UUID SEED_EM_ANALISE =
      UUID.fromString("ee100000-0000-4000-8000-000000000001");
  private static final UUID SEED_NEGADA = UUID.fromString("ee100000-0000-4000-8000-000000000003");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private RecordingMailSender mail;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update("delete from notification where account_id = ?::uuid", MARIA_ACCOUNT_ID);
    mail.messages.clear();
    restoreSeedGuide();
    // Guides created by this suite's own tests carry a GD-SIM-* number, never colliding with the
    // V23 seed's GD-0000000X numbers.
    jdbc.update(
        "delete from guide_item where guide_id in (select id from guide where number like"
            + " 'GD-SIM-%')");
    jdbc.update("delete from guide where number like 'GD-SIM-%'");
  }

  @BeforeEach
  void ensureOperatorCredential() {
    jdbc.update(
        "insert into beneficiary"
            + " (id, plan_id, full_name, cpf, cns, card_number, birth_date, role, titular_id,"
            + " active)"
            + " values (?::uuid, ?::uuid, 'OPERADOR SIMULACAO', '52999999999', '700000000000099',"
            + " '009000009', date '1980-01-01', 'TITULAR', null, true) on conflict (id) do nothing",
        OPERATOR_BEN,
        PLAN_ID);
    jdbc.update(
        "insert into user_account (id, beneficiary_id, email, password_hash, status, created_at)"
            + " values (?::uuid, ?::uuid, ?, '{noop}operador12345', 'ACTIVE', now())"
            + " on conflict (id) do nothing",
        OPERATOR_ACC,
        OPERATOR_BEN,
        OPERATOR_EMAIL);
  }

  private void restoreSeedGuide() {
    jdbc.update(
        "update guide set status = 'EM_ANALISE', auth_password = null, auth_valid_until = null,"
            + " denial_reason = null where id = ?::uuid",
        SEED_EM_ANALISE);
    jdbc.update(
        "update guide_item set status = 'EM_ANALISE' where guide_id = ?::uuid", SEED_EM_ANALISE);
  }

  @Test
  void beneficiaryAccount_callingASimGuidesRoute_returns403_forbidden() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/guides")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createGuideBody("GD-SIM-FORBIDDEN"))
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("sim.forbidden"));
  }

  @Test
  void authorize_theSeededGuide_notifiesAndShowsThePassword_ac7() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/guides/{id}/authorize", SEED_EM_ANALISE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"AUT-482913\",\"validUntil\":\"2026-12-31\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("AUTORIZADA"));

    await(() -> mariaNotificationCount() == 1 && mail.messages.size() == 1);
    assertThat(
            jdbc.queryForObject(
                "select event_type_code from notification where account_id = ?::uuid",
                String.class,
                MARIA_ACCOUNT_ID))
        .isEqualTo("guide.status-changed");

    // Indistinguishable from a real back-office authorization (SPEC-0018 AC1 / SPEC-0012 AC7).
    mockMvc
        .perform(
            get("/api/guides/{id}", SEED_EM_ANALISE)
                .param("beneficiaryId", MARIA_ID.toString())
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("AUTORIZADA"))
        .andExpect(jsonPath("$.authPassword").value("AUT-482913"));
  }

  @Test
  void authorize_aFinalStateGuide_returns409_invalidTransition() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/guides/{id}/authorize", SEED_NEGADA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"AUT-000000\",\"validUntil\":\"2026-12-31\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("sim.invalid-transition"));
  }

  @Test
  void authorize_unknownGuide_returns404_targetNotFound() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/guides/{id}/authorize", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"AUT-000000\",\"validUntil\":\"2026-12-31\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("sim.target-not-found"));
  }

  @Test
  void createGuide_opensEmAnalise() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/guides")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createGuideBody("GD-SIM-CREATE"))
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("EM_ANALISE"))
        .andExpect(jsonPath("$.number").isNotEmpty())
        .andExpect(jsonPath("$.id").isNotEmpty());

    assertThat(
            jdbc.queryForObject(
                "select count(*) from guide where beneficiary_id = ?::uuid"
                    + " and status = 'EM_ANALISE' and requesting_provider = 'Dr. Sim Testador'",
                Long.class,
                MARIA_ID))
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void partiallyAuthorize_derivesPartialStatus_br6() throws Exception {
    UUID guideId = createFreshGuide();

    mockMvc
        .perform(
            post("/api/sim/guides/{id}/partially-authorize", guideId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"password\":\"AUT-111111\",\"validUntil\":\"2026-12-31\",\"itemStatuses\":"
                        + "[{\"tussCode\":\"10101012\",\"status\":\"AUTORIZADO\"}]}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("AUTORIZADA"));
  }

  @Test
  void deny_setsNegadaWithReason() throws Exception {
    UUID guideId = createFreshGuide();

    mockMvc
        .perform(
            post("/api/sim/guides/{id}/deny", guideId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Procedimento não coberto pelo plano\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NEGADA"));

    assertThat(
            jdbc.queryForObject(
                "select denial_reason from guide where id = ?::uuid", String.class, guideId))
        .isEqualTo("Procedimento não coberto pelo plano");
  }

  @Test
  void cancel_thenMarkExecuted_returns409_invalidTransition() throws Exception {
    UUID guideId = createFreshGuide();

    mockMvc
        .perform(post("/api/sim/guides/{id}/cancel", guideId).with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELADA"));

    mockMvc
        .perform(post("/api/sim/guides/{id}/mark-executed", guideId).with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("sim.invalid-transition"));
  }

  @Test
  void authorize_thenMarkExecuted_setsExecutada() throws Exception {
    UUID guideId = createFreshGuide();
    mockMvc
        .perform(
            post("/api/sim/guides/{id}/authorize", guideId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"AUT-222222\",\"validUntil\":\"2026-12-31\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/sim/guides/{id}/mark-executed", guideId).with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EXECUTADA"));
  }

  private UUID createFreshGuide() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/sim/guides")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createGuideBody("GD-SIM-" + UUID.randomUUID()))
                    .with(authAs(OPERATOR_EMAIL)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(com.jayway.jsonpath.JsonPath.read(body, "$.id").toString());
  }

  private static String createGuideBody(String marker) {
    return "{\"beneficiaryId\":\""
        + MARIA_ID
        + "\",\"type\":\"CONSULTA\",\"requestingProvider\":\"Dr. Sim Testador\","
        + "\"items\":[{\"tussCode\":\"10101012\",\"description\":\"Consulta - "
        + marker
        + "\",\"quantity\":1}]}";
  }

  private long mariaNotificationCount() {
    return jdbc.queryForObject(
        "select count(*) from notification where account_id = ?::uuid",
        Long.class,
        MARIA_ACCOUNT_ID);
  }

  private static void await(BooleanSupplier condition) {
    for (int attempt = 0; attempt < 100; attempt++) {
      if (condition.getAsBoolean()) {
        return;
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
    throw new AssertionError("guide status-changed notification not delivered in time");
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
