package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fkmed.domain.telemedicine.EnterQueueCommand;
import com.fkmed.domain.telemedicine.TeleService;
import java.util.List;
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
 * SPEC-0018 tele slice over MockMvc + Testcontainers Postgres (ADR-0017/DL-0021): the flag-gated
 * {@code /api/sim/**} guard rails (403 for a beneficiary — BR2; 409 on an invalid transition —
 * BR4), the atomic closure (session ENCERRADA + its documents issued in one transaction, bound to
 * the beneficiary + session — SPEC-0010 BR10), the referral specialty-name resolution from the
 * network registry, and the cross-spec event → notification wiring (turn/closed/document → in-app +
 * e-mail). The sim is enabled by the dev profile; the operator credential is ensured in
 * {@code @BeforeEach} (sibling ITs clean every non-MARIA account). Tele/document/notification
 * tables are cleaned for isolation on the shared Postgres.
 */
@Import(RecordingMailConfig.class)
class OperatorSimTeleIT extends AbstractIntegrationTest {

  private static final String OPERATOR_EMAIL = "operador-sim@fkmed.local";
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_CARD = "001234567";
  private static final UUID MARIA_ID = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");
  private static final UUID MARIA_ACCOUNT_ID =
      UUID.fromString("d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70");
  private static final UUID OPERATOR_BEN = UUID.fromString("c0000000-0000-4000-8000-000000000001");
  private static final UUID OPERATOR_ACC = UUID.fromString("c0000000-0000-4000-8000-000000000002");
  private static final UUID PLAN_ID = UUID.fromString("b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a");

  @Autowired private MockMvc mockMvc;
  @Autowired private TeleService tele;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private RecordingMailSender mail;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update("delete from notification");
    // Delete ONLY the documents this IT creates — issued through a real tele session, or
    // operator-issued via /api/sim/documents — never the V18 seed (whose origin_session_id is a
    // placeholder not present in tele_session, and origin_operator_id is null).
    // ClinicalDocumentSeedIT
    // asserts the seed survives sibling ITs on the shared Postgres (test isolation, checklist §8).
    String testDocs =
        "select id from clinical_document where origin_session_id in (select id from tele_session)"
            + " or origin_operator_id is not null";
    jdbc.update("delete from exam_order_item where document_id in (" + testDocs + ")");
    jdbc.update("delete from prescription_item where document_id in (" + testDocs + ")");
    jdbc.update("delete from clinical_document where id in (" + testDocs + ")");
    jdbc.update("delete from tele_session_symptom");
    jdbc.update("delete from tele_session");
    mail.messages.clear();
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

  @Test
  void beneficiaryAccount_callingASimRoute_returns403_forbidden() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/tele/sessions/next/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professionalName\":\"Dra. Ana\",\"crm\":\"CRM-RJ 1\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("sim.forbidden"));
  }

  @Test
  void startNext_thenClose_issuesDocumentsAtomically_andNotifies() throws Exception {
    UUID sessionId = enterQueue();

    mockMvc
        .perform(
            post("/api/sim/tele/sessions/next/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professionalName\":\"Dra. Ana\",\"crm\":\"CRM-RJ 12345\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
        .andExpect(jsonPath("$.state").value("EM_ATENDIMENTO"));

    String closeBody =
        "{\"professionalName\":\"Dra. Ana\",\"crm\":\"CRM-RJ 12345\","
            + "\"guidance\":\"Repouso e hidratação\",\"documents\":["
            + "{\"type\":\"PRESCRIPTION\",\"medications\":[{\"medication\":\"Ibuprofeno 600mg\","
            + "\"posology\":\"1cp 12/12h\",\"guidance\":\"Com alimentos\"}]},"
            + "{\"type\":\"REFERRAL\",\"specialtyCode\":\"CARDIOLOGIA\","
            + "\"reason\":\"Palpitações recorrentes\"}]}";
    mockMvc
        .perform(
            post("/api/sim/tele/sessions/{id}/close", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(closeBody)
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("ENCERRADA"))
        .andExpect(jsonPath("$.issuedDocumentIds.length()").value(2));

    // BR10 atomicity: the session is closed AND both documents are visible in clinicaldocs, bound
    // to
    // the attended beneficiary and to the session origin.
    assertThat(state(sessionId)).isEqualTo("ENCERRADA");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from clinical_document where origin_session_id = ?::uuid"
                    + " and beneficiary_id = ?::uuid",
                Long.class,
                sessionId,
                MARIA_ID))
        .isEqualTo(2);
    // C.4 critical: the referral's specialty name is resolved from the domain.network registry.
    assertThat(
            jdbc.queryForObject(
                "select target_specialty_name from clinical_document"
                    + " where type = 'REFERRAL' and origin_session_id = ?::uuid",
                String.class,
                sessionId))
        .isEqualTo("Cardiologia");
    // BR3: the sim action is audited with the operator as author.
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_event where event_type = 'sim.operator-action'"
                    + " and author_account_id = ?::uuid",
                Long.class,
                OPERATOR_ACC))
        .isGreaterThanOrEqualTo(1);

    // Cross-spec event → notification: turn (start) + closed + two documents-issued, each e-mailed.
    await(() -> mariaNotificationCount() == 4 && mail.messages.size() == 4);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from notification where account_id = ?::uuid"
                    + " and event_type_code = 'clinical-document.issued'",
                Long.class,
                MARIA_ACCOUNT_ID))
        .isEqualTo(2);
  }

  @Test
  void close_aQueuedSession_returns409_invalidTransition() throws Exception {
    UUID sessionId = enterQueue();

    mockMvc
        .perform(
            post("/api/sim/tele/sessions/{id}/close", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professionalName\":\"Dra. Ana\",\"crm\":\"CRM-RJ 1\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("sim.invalid-transition"));

    assertThat(state(sessionId)).isEqualTo("EM_FILA");
  }

  @Test
  void startNext_withAnEmptyQueue_returns404_targetNotFound() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/tele/sessions/next/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professionalName\":\"Dra. Ana\",\"crm\":\"CRM-RJ 1\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("sim.target-not-found"));
  }

  @Test
  void documentsEndpoint_issuesAnOperatorDocument() throws Exception {
    String body =
        "{\"beneficiaryId\":\""
            + MARIA_ID
            + "\",\"professionalName\":\"Dr. Operador\",\"crm\":\"CRM-RJ 999\","
            + "\"type\":\"SICK_NOTE\",\"periodStart\":\"2026-07-06\",\"periodEnd\":\"2026-07-08\","
            + "\"cid\":\"J11\",\"notes\":\"Repouso\"}";
    mockMvc
        .perform(
            post("/api/sim/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documentId").isNotEmpty());

    assertThat(
            jdbc.queryForObject(
                "select count(*) from clinical_document where beneficiary_id = ?::uuid"
                    + " and origin_operator_id = ?::uuid and type = 'SICK_NOTE'",
                Long.class,
                MARIA_ID,
                OPERATOR_ACC))
        .isEqualTo(1);
  }

  private UUID enterQueue() {
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

  private String state(UUID sessionId) {
    return jdbc.queryForObject(
        "select state from tele_session where id = ?::uuid", String.class, sessionId);
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
    throw new AssertionError("sim notifications/e-mails not delivered in time");
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
