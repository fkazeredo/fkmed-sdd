package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
 * SPEC-0013 operator-sim finance over MockMvc + Testcontainers (SPEC-0018): issue → {@code
 * InvoiceIssued} → the titular's notification + e-mail; the idempotent payment (BR6 — pay twice
 * leaves exactly one paid invoice and exactly one issuance event); a copay record; and the 403 for
 * a beneficiary. The finance write-path rows this IT creates use random UUIDs; the V24 seed (fixed
 * {@code fa…}/{@code fc…} ids) is preserved, so {@code FinanceApiIT} still sees a clean seed.
 */
@Import(RecordingMailConfig.class)
class OperatorSimFinanceIT extends AbstractIntegrationTest {

  private static final String OPERATOR_EMAIL = "operador-sim@fkmed.local";
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final UUID MARIA_ID = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");
  private static final UUID PEDRO_ID = UUID.fromString("9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");
  private static final UUID MARIA_ACCOUNT_ID =
      UUID.fromString("d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70");
  private static final UUID OPERATOR_BEN = UUID.fromString("c0000000-0000-4000-8000-000000000001");
  private static final UUID OPERATOR_ACC = UUID.fromString("c0000000-0000-4000-8000-000000000002");
  private static final UUID PLAN_ID = UUID.fromString("b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a");
  private static final String NEW_LINE = "55555555555555555555555555555555555555555555555";
  private static final String NEW_LINE_2 = "66666666666666666666666666666666666666666666666";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private RecordingMailSender mail;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update("delete from notification where event_type_code = 'finance.invoice-issued'");
    jdbc.update("delete from invoice where id::text not like 'fa000000-0000-4000-8000-%'");
    jdbc.update("delete from copay_entry where id::text not like 'fc000000-0000-4000-8000-%'");
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
  void issueInvoice_returnsOpen_publishesInvoiceIssued_andNotifiesTheTitular() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/finance/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(issueBody(NEW_LINE))
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.competencia").value("Janeiro/2027"))
        .andExpect(jsonPath("$.id").isNotEmpty());

    await(() -> financeNotificationCount() == 1 && mail.messages.size() == 1);
    assertThat(financeNotificationCount()).isEqualTo(1);
  }

  @Test
  void payingTwice_leavesExactlyOnePaidInvoice_andOneIssuanceEvent() throws Exception {
    String id =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(
                    post("/api/sim/finance/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueBody(NEW_LINE_2))
                        .with(authAs(OPERATOR_EMAIL)))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");
    await(() -> financeNotificationCount() == 1);

    mockMvc
        .perform(post("/api/sim/finance/invoices/{id}/pay", id).with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk());
    Instant firstPaidAt = paidAt(id);
    assertThat(firstPaidAt).isNotNull();

    mockMvc
        .perform(post("/api/sim/finance/invoices/{id}/pay", id).with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isOk());

    // BR6 idempotency: still exactly one issuance event/notification, paid_at unchanged.
    assertThat(financeNotificationCount()).isEqualTo(1);
    assertThat(paidAt(id)).isEqualTo(firstPaidAt);
  }

  @Test
  void payingAnUnknownInvoice_returns404_targetNotFound() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/finance/invoices/{id}/pay", UUID.randomUUID())
                .with(authAs(OPERATOR_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("sim.target-not-found"));
  }

  @Test
  void recordCopay_createsTheEntry() throws Exception {
    String body =
        "{\"entryDate\":\"2026-07-01\",\"procedure\":\"Consulta — Nutrição\",\"provider\":\"Clínica"
            + " NutreBem\",\"beneficiaryId\":\""
            + PEDRO_ID
            + "\",\"amount\":38.00}";
    String id =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(
                    post("/api/sim/finance/copay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(authAs(OPERATOR_EMAIL)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");

    assertThat(
            jdbc.queryForObject(
                "select count(*) from copay_entry where id = ?::uuid", Long.class, id))
        .isEqualTo(1);
  }

  @Test
  void aBeneficiary_callingASimFinanceRoute_is403() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/finance/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(issueBody(NEW_LINE))
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("sim.forbidden"));
  }

  private static String issueBody(String line) {
    return "{\"titularBeneficiaryId\":\""
        + MARIA_ID
        + "\",\"competencia\":\"2027-01\",\"dueDate\":\"2027-01-10\",\"amount\":300.00,"
        + "\"digitableLine\":\""
        + line
        + "\",\"pixCode\":\"00020126-pix-sim-code-52040000530398654053005802BR6304ABCD\"}";
  }

  private Instant paidAt(String id) {
    return jdbc.queryForObject("select paid_at from invoice where id = ?::uuid", Instant.class, id);
  }

  private long financeNotificationCount() {
    return jdbc.queryForObject(
        "select count(*) from notification where account_id = ?::uuid"
            + " and event_type_code = 'finance.invoice-issued'",
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
    throw new AssertionError("sim finance notification/e-mail not delivered in time");
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
