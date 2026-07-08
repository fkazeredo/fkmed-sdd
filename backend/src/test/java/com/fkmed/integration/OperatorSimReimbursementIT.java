package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class OperatorSimReimbursementIT extends AbstractIntegrationTest {

  private static final ZoneId PRODUCT_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String OPERATOR_EMAIL = "operador-sim@fkmed.local";
  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final UUID MARIA_ACCOUNT_ID =
      UUID.fromString("d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70");
  private static final UUID SEED_DENIED = UUID.fromString("10000000-0000-4000-8000-000000000004");
  private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31};

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update(
        "delete from notification where account_id = ?::uuid and event_type_code like"
            + " 'reimbursement.%'",
        MARIA_ACCOUNT_ID);
    jdbc.update(
        "delete from reimbursement_timeline_event where request_id in"
            + " (select id from reimbursement_request where idempotency_key like 'it-sim-%')");
    jdbc.update(
        "delete from reimbursement_document where request_id in"
            + " (select id from reimbursement_request where idempotency_key like 'it-sim-%')");
    jdbc.update(
        "delete from reimbursement_session_item where request_id in"
            + " (select id from reimbursement_request where idempotency_key like 'it-sim-%')");
    jdbc.update("delete from reimbursement_request where idempotency_key like 'it-sim-%'");
  }

  @Test
  void approveAndPayTwice_createsASinglePaidTransitionAndNotification() throws Exception {
    UUID id = createRequest("it-sim-pay-" + UUID.randomUUID());

    mockMvc
        .perform(post("/api/sim/reimbursements/{id}/approve", id).with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APROVADO"));

    mockMvc
        .perform(
            post("/api/sim/reimbursements/{id}/pay", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outcome\":\"SUCCESS\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAGO"));

    mockMvc
        .perform(
            post("/api/sim/reimbursements/{id}/pay", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outcome\":\"SUCCESS\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAGO"));

    assertThat(timelineCount(id, "PAGO")).isEqualTo(1);
    await(() -> notificationCount("reimbursement.paid") == 1);
  }

  @Test
  void deniedRequest_cannotBePaidThroughSim() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/reimbursements/{id}/pay", SEED_DENIED)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outcome\":\"SUCCESS\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("sim.invalid-transition"));

    assertThat(statusOf(SEED_DENIED)).isEqualTo("NEGADO");
  }

  @Test
  void paymentFailure_thenBeneficiaryBankCorrection_movesToPaid() throws Exception {
    UUID id = createRequest("it-sim-bank-" + UUID.randomUUID());
    approve(id);

    mockMvc
        .perform(
            post("/api/sim/reimbursements/{id}/pay", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outcome\":\"FAILURE\",\"failureReason\":\"Conta recusada\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAGAMENTO_NAO_EFETUADO"));

    mockMvc
        .perform(get("/api/reimbursements/{id}", id).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAGAMENTO_NAO_EFETUADO"));

    mockMvc
        .perform(
            post("/api/reimbursements/{id}/bank-correction", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"bankCode\":\"001\",\"bankAgency\":\"1234\",\"bankAccount\":\"654321\","
                        + "\"bankAccountDigit\":\"9\",\"bankAccountType\":\"CORRENTE\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAGO"));
  }

  @Test
  void openPendency_thenBeneficiaryUploadsDocument_returnsToProcessing() throws Exception {
    UUID id = createRequest("it-sim-pendency-" + UUID.randomUUID());

    mockMvc
        .perform(
            post("/api/sim/reimbursements/{id}/pendency", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Pedido medico ilegivel - reenviar\"}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDENTE_DOCUMENTACAO"));

    mockMvc
        .perform(get("/api/reimbursements/{id}", id).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendency.description").value("Pedido medico ilegivel - reenviar"));

    mockMvc
        .perform(pendencyDocuments(id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PROCESSAMENTO"));
  }

  @Test
  void beneficiaryCallingASimReimbursementRoute_returns403() throws Exception {
    UUID id = createRequest("it-sim-forbidden-" + UUID.randomUUID());

    mockMvc
        .perform(post("/api/sim/reimbursements/{id}/approve", id).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("sim.forbidden"));
  }

  private UUID createRequest(String idempotencyKey) throws Exception {
    LocalDate careDate = LocalDate.now(PRODUCT_ZONE).minusMonths(1);
    mockMvc
        .perform(consultationRequest(careDate, idempotencyKey))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PROCESSAMENTO"));
    return jdbc.queryForObject(
        "select id from reimbursement_request where idempotency_key = ?",
        UUID.class,
        idempotencyKey);
  }

  private void approve(UUID id) throws Exception {
    mockMvc
        .perform(post("/api/sim/reimbursements/{id}/approve", id).with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APROVADO"));
  }

  private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
      consultationRequest(LocalDate careDate, String idempotencyKey) {
    var builder =
        multipart("/api/reimbursements")
            .file(
                new MockMultipartFile(
                    "request",
                    "request.json",
                    MediaType.APPLICATION_JSON_VALUE,
                    consultationJson(careDate).getBytes(StandardCharsets.UTF_8)))
            .file(new MockMultipartFile("documents", "receipt.pdf", "application/pdf", PDF));
    builder.header("Idempotency-Key", idempotencyKey);
    builder.with(authAs(MARIA_EMAIL));
    return builder;
  }

  private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
      pendencyDocuments(UUID id) {
    return multipart("/api/reimbursements/{id}/pendency-documents", id)
        .file(
            new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"documents\":[{\"category\":\"MEDICAL_ORDER\",\"fileName\":\"pedido.pdf\"}]}"
                    .getBytes(StandardCharsets.UTF_8)))
        .file(new MockMultipartFile("documents", "pedido.pdf", "application/pdf", PDF))
        .with(authAs(MARIA_EMAIL));
  }

  private static String consultationJson(LocalDate careDate) {
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
          "providerSpecialty": "Clinica medica",
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
        .formatted(MARIA_ID, careDate);
  }

  private long timelineCount(UUID id, String status) {
    return jdbc.queryForObject(
        "select count(*) from reimbursement_timeline_event where request_id = ?::uuid and status = ?",
        Long.class,
        id,
        status);
  }

  private long notificationCount(String eventType) {
    return jdbc.queryForObject(
        "select count(*) from notification where account_id = ?::uuid and event_type_code = ?",
        Long.class,
        MARIA_ACCOUNT_ID,
        eventType);
  }

  private String statusOf(UUID id) {
    return jdbc.queryForObject(
        "select status from reimbursement_request where id = ?::uuid", String.class, id);
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
    throw new AssertionError("condition was not met in time");
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
