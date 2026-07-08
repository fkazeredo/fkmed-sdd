package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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

class ReimbursementPreviewApiIT extends AbstractIntegrationTest {

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String OPERATOR_EMAIL = "operador-sim@fkmed.local";
  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final UUID MARIA_ACCOUNT_ID =
      UUID.fromString("d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70");
  private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31};

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update(
        "delete from notification where account_id = ?::uuid and event_type_code ="
            + " 'preview.concluded'",
        MARIA_ACCOUNT_ID);
    jdbc.update(
        "delete from preview_document where preview_id in"
            + " (select id from reimbursement_preview where created_by = ?::uuid)",
        MARIA_ACCOUNT_ID);
    jdbc.update("delete from reimbursement_preview where created_by = ?::uuid", MARIA_ACCOUNT_ID);
  }

  @Test
  void consultationPreview_isImmediateWithDisclaimerAndNotification() throws Exception {
    mockMvc
        .perform(previewRequest("CONSULTA", "[]"))
        .andExpect(status().isCreated())
        .andExpect(
            jsonPath("$.protocol")
                .value(org.hamcrest.Matchers.matchesPattern("^PV-\\d{8}-\\d{4}$")))
        .andExpect(jsonPath("$.situation").value("CONCLUIDA"))
        .andExpect(jsonPath("$.estimatedValue").value(120.00))
        .andExpect(
            jsonPath("$.disclaimer").value(org.hamcrest.Matchers.containsString("Não representa")));

    await(() -> notificationCount() == 1);
  }

  @Test
  void examPreviewWithoutBudget_returns422() throws Exception {
    mockMvc
        .perform(previewRequest("EXAME", "[]"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("preview.attachments-required"));
  }

  @Test
  void analyzedPreview_canBeConcludedThroughSim() throws Exception {
    String body =
        mockMvc
            .perform(
                previewRequest(
                        "EXAME",
                        "[{\"category\":\"BUDGET\",\"fileName\":\"orcamento.pdf\"},"
                            + "{\"category\":\"MEDICAL_ORDER\",\"fileName\":\"pedido.pdf\"}]")
                    .file(
                        new MockMultipartFile("documents", "orcamento.pdf", "application/pdf", PDF))
                    .file(new MockMultipartFile("documents", "pedido.pdf", "application/pdf", PDF)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.situation").value("EM_ANALISE"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");

    mockMvc
        .perform(
            post("/api/sim/reimbursement-previews/{id}/conclude", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estimatedValue\":80.00}")
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.situation").value("CONCLUIDA"))
        .andExpect(jsonPath("$.estimatedValue").value(80.00));

    assertThat(
            jdbc.queryForObject(
                "select situation from reimbursement_preview where id = ?::uuid", String.class, id))
        .isEqualTo("CONCLUIDA");
  }

  private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
      previewRequest(String expenseType, String metadata) {
    return multipart("/api/reimbursement-previews")
        .file(
            new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {"beneficiaryId":"%s","expenseTypeCode":"%s","documents":%s}
                """
                    .formatted(MARIA_ID, expenseType, metadata)
                    .getBytes(StandardCharsets.UTF_8)))
        .with(authAs(MARIA_EMAIL));
  }

  private long notificationCount() {
    return jdbc.queryForObject(
        "select count(*) from notification where account_id = ?::uuid and event_type_code ="
            + " 'preview.concluded'",
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
    throw new AssertionError("preview notification not delivered in time");
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
