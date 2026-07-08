package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0015 API contract over MockMvc and Testcontainers Postgres: full reimbursement submission
 * (protocol/status/timeline/notification), idempotent retry through {@code Idempotency-Key}, and
 * server-side 422 validation. Tables are cleaned before and after each test because the suite
 * shares a singleton Postgres container.
 */
class ReimbursementApiIT extends AbstractIntegrationTest {

  private static final ZoneId PRODUCT_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final String MARIA_ACCOUNT_ID = "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70";
  private static final String NO_REIMBURSEMENT_EMAIL = "reembolso-sem-direito-e2e@fkmed.local";
  private static final String NO_REIMBURSEMENT_ID = "a2b3c4d5-e6f7-4a8b-9c0d-1e2f3a4b5c6e";

  private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31}; // %PDF-1

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update(
        "delete from notification where account_id = ?::uuid and event_type_code ="
            + " 'reimbursement.submitted'",
        MARIA_ACCOUNT_ID);
    jdbc.update(
        "delete from reimbursement_timeline_event where request_id in"
            + " (select id from reimbursement_request where idempotency_key like 'it-%')");
    jdbc.update(
        "delete from reimbursement_document where request_id in"
            + " (select id from reimbursement_request where idempotency_key like 'it-%')");
    jdbc.update(
        "delete from reimbursement_session_item where request_id in"
            + " (select id from reimbursement_request where idempotency_key like 'it-%')");
    jdbc.update("delete from reimbursement_request where idempotency_key like 'it-%'");
  }

  @Test
  void submitConsultation_returnsProtocolStatusTimelineAndNotification_retryIsIdempotent()
      throws Exception {
    String idempotencyKey = "it-consulta-" + java.util.UUID.randomUUID();
    LocalDate careDate = LocalDate.now(PRODUCT_ZONE).minusMonths(1);

    String first =
        mockMvc
            .perform(consultationRequest(MARIA_ID, careDate, idempotencyKey, MARIA_EMAIL))
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.protocol")
                    .value(org.hamcrest.Matchers.matchesPattern("^RE-\\d{8}-\\d{4}$")))
            .andExpect(jsonPath("$.status").value("PROCESSAMENTO"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String protocol = first.replaceAll(".*\"protocol\"\\s*:\\s*\"([^\"]+)\".*", "$1");

    mockMvc
        .perform(consultationRequest(MARIA_ID, careDate, idempotencyKey, MARIA_EMAIL))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.protocol").value(protocol))
        .andExpect(jsonPath("$.status").value("PROCESSAMENTO"));

    assertThat(requestsWith(idempotencyKey)).isEqualTo(1);
    assertThat(timelineEventsFor(protocol)).isEqualTo(2);
    awaitSubmittedNotification();
  }

  @Test
  void seededPaidDetailAndStatementExposeGlosaAndJuneTotal() throws Exception {
    mockMvc
        .perform(
            get("/api/reimbursements/10000000-0000-4000-8000-000000000001")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAGO"))
        .andExpect(jsonPath("$.amountRequested").value(150.00))
        .andExpect(jsonPath("$.amountReimbursed").value(120.00))
        .andExpect(jsonPath("$.glosa.amount").value(30.00))
        .andExpect(jsonPath("$.glosa.reason").value("Valor excede a tabela do plano"));

    mockMvc
        .perform(
            get("/api/reimbursements/statement")
                .param("from", "2026-06-01")
                .param("to", "2026-06-30")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(120.00))
        .andExpect(jsonPath("$.items[0].protocol").value("RE-20260601-0001"));
  }

  @Test
  void submitWithExpiredCareDate_returns422DeadlineExpired() throws Exception {
    mockMvc
        .perform(
            consultationRequest(
                MARIA_ID,
                LocalDate.now(PRODUCT_ZONE).minusMonths(14),
                "it-expired-" + java.util.UUID.randomUUID(),
                MARIA_EMAIL))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("reimbursement.deadline-expired"));
  }

  @Test
  void submitWhenPlanHasNoReimbursement_returns403NotEligible() throws Exception {
    mockMvc
        .perform(
            consultationRequest(
                NO_REIMBURSEMENT_ID,
                LocalDate.now(PRODUCT_ZONE).minusMonths(1),
                "it-no-right-" + java.util.UUID.randomUUID(),
                NO_REIMBURSEMENT_EMAIL))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("reimbursement.not-eligible"));
  }

  private MockMultipartHttpServletRequestBuilder consultationRequest(
      String beneficiaryId, LocalDate careDate, String idempotencyKey, String email) {
    var builder =
        multipart("/api/reimbursements")
            .file(
                new MockMultipartFile(
                    "request",
                    "request.json",
                    MediaType.APPLICATION_JSON_VALUE,
                    consultationJson(beneficiaryId, careDate).getBytes(StandardCharsets.UTF_8)))
            .file(new MockMultipartFile("documents", "receipt.pdf", "application/pdf", PDF));
    builder.header("Idempotency-Key", idempotencyKey);
    builder.with(authAs(email));
    return builder;
  }

  private static String consultationJson(String beneficiaryId, LocalDate careDate) {
    return """
        {
          "beneficiaryId": "%s",
          "expenseTypeCode": "CONSULTA",
          "careDate": "%s",
          "amount": 150.00,
          "providerName": "Clinica Livre Escolha",
          "providerCouncilCode": "CRM",
          "providerCouncilNumber": "123456",
          "providerCouncilUf": "SP",
          "providerDocument": "39053344705",
          "providerSpecialty": "Clínica médica",
          "bankCode": "001",
          "bankAgency": "1234",
          "bankAccount": "123456",
          "bankAccountDigit": "7",
          "bankAccountType": "CORRENTE",
          "acceptedTermVersion": "1.0",
          "documents": [
            {"category": "RECEIPT", "fileName": "receipt.pdf"}
          ],
          "sessions": []
        }
        """
        .formatted(beneficiaryId, careDate);
  }

  private long requestsWith(String idempotencyKey) {
    return jdbc.queryForObject(
        "select count(*) from reimbursement_request where idempotency_key = ?",
        Long.class,
        idempotencyKey);
  }

  private long timelineEventsFor(String protocol) {
    return jdbc.queryForObject(
        "select count(*) from reimbursement_timeline_event e"
            + " join reimbursement_request r on r.id = e.request_id"
            + " where r.protocol = ?",
        Long.class,
        protocol);
  }

  private void awaitSubmittedNotification() {
    for (int attempt = 0; attempt < 100; attempt++) {
      Long count =
          jdbc.queryForObject(
              "select count(*) from notification where account_id = ?::uuid"
                  + " and event_type_code = 'reimbursement.submitted'",
              Long.class,
              MARIA_ACCOUNT_ID);
      if (count != null && count == 1) {
        return;
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
    throw new AssertionError("reimbursement submitted notification was not created");
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
