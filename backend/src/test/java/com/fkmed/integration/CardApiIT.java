package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0007 (Digital Card): card + data sheet, PDF download, family-scope reuse (404), the BR10
 * inactive state (409) and the BR7 dependent-view audit entry. The caller's card is resolved
 * server-side from the authenticated account (ADR-0009): MARIA is seeded by Flyway V3, PEDRO's
 * account is a disposable fixture created here.
 */
class CardApiIT extends AbstractIntegrationTest {

  private static final String MARIA_CARD = "001234567";
  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_ACCOUNT_ID = "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70";
  private static final String PEDRO_CARD = "001234575";
  private static final String PEDRO_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";
  private static final String PEDRO_EMAIL = PedroAccountFixture.PEDRO_EMAIL;
  private static final String PLAN_ID = "b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a";
  private static final String PLAN_NAME = "PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP";

  // Dedicated, disposable inactive-dependent fixture for BR10 (tests create their own data —
  // docs/architecture/testing.md — rather than mutating the canonical MARIA/PEDRO seed that other
  // ITs depend on being active).
  private static final String INACTIVE_ID = "aa11bb22-cc33-4d44-8e55-9f66a0b1c2d3";
  private static final String INACTIVE_CARD = "001234580";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    // Isolation (docs/architecture/testing.md): clean in @BeforeEach, not only @AfterEach —
    // Postgres is a singleton for the whole suite.
    jdbc.update("delete from audit_event where event_type = 'card.dependent-viewed'");
    jdbc.update("delete from beneficiary where id = ?::uuid", INACTIVE_ID);
    jdbc.update(
        "insert into beneficiary"
            + " (id, plan_id, full_name, cpf, cns, card_number, birth_date, role, titular_id,"
            + " active)"
            + " values (?::uuid, ?::uuid, 'BEATRIZ SOUZA LIMA', '39053344705',"
            + " '700000000000005', ?, '2010-01-01', 'DEPENDENT', ?::uuid, false)",
        INACTIVE_ID,
        PLAN_ID,
        INACTIVE_CARD,
        MARIA_ID);
    // PEDRO (dependent) has no Flyway-seeded account; a disposable one lets server-side card
    // resolution (ADR-0009) yield PEDRO_CARD.
    PedroAccountFixture.seed(jdbc);
  }

  @AfterEach
  void removePedroAccount() {
    PedroAccountFixture.remove(jdbc);
  }

  @Test
  void card_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(get("/api/cards/{id}", MARIA_ID)).andExpect(status().isUnauthorized());
  }

  @Test
  void card_asMaria_ofHerself_returnsAc1Values() throws Exception {
    mockMvc
        .perform(get("/api/cards/{id}", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("MARIA CLARA SOUZA LIMA"))
        .andExpect(jsonPath("$.cardNumber").value(MARIA_CARD))
        .andExpect(jsonPath("$.cns").value("700000000000001"))
        .andExpect(jsonPath("$.ansRegistration").value("326305"))
        .andExpect(jsonPath("$.coverage").value("ESTADUAL"))
        .andExpect(jsonPath("$.planName").value(PLAN_NAME))
        .andExpect(jsonPath("$.planCategory").value("Coletivo por Adesão"))
        .andExpect(jsonPath("$.additives[0]").value("Urg/emerg Nacional Hr — Assistência"));
  }

  @Test
  void card_asMaria_ofPedro_returnsPedrosCard_andRecordsDependentViewAudit() throws Exception {
    long before = countAuditEvents();

    mockMvc
        .perform(get("/api/cards/{id}", PEDRO_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("PEDRO SOUZA LIMA"))
        .andExpect(jsonPath("$.cardNumber").value(PEDRO_CARD))
        .andExpect(jsonPath("$.cns").value("700000000000002"));

    assertThat(countAuditEvents()).isEqualTo(before + 1);
    var row =
        jdbc.queryForMap(
            "select * from audit_event where event_type = 'card.dependent-viewed'"
                + " order by occurred_at desc limit 1");
    assertThat(row.get("author_account_id").toString()).isEqualTo(MARIA_ACCOUNT_ID);
    assertThat(row.get("target_beneficiary_id").toString()).isEqualTo(PEDRO_ID);
  }

  @Test
  void card_asMaria_ofHerself_recordsNoAudit() throws Exception {
    long before = countAuditEvents();

    mockMvc
        .perform(get("/api/cards/{id}", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk());

    assertThat(countAuditEvents()).isEqualTo(before);
  }

  @Test
  void card_asPedro_ofMaria_returns404WithoutRevealingExistence() throws Exception {
    mockMvc
        .perform(get("/api/cards/{id}", MARIA_ID).with(authAs(PEDRO_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"));
  }

  @Test
  void card_ofUnknownId_returns404() throws Exception {
    mockMvc
        .perform(
            get("/api/cards/{id}", "00000000-0000-0000-0000-000000000000")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"));
  }

  @Test
  void card_ofInactiveBeneficiary_returns409CardUnavailable() throws Exception {
    mockMvc
        .perform(get("/api/cards/{id}", INACTIVE_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("card.unavailable"))
        .andExpect(
            jsonPath("$.message")
                .value("Carteirinha indisponível — contate os canais de atendimento."));
  }

  @Test
  void cardPdf_asMaria_ofHerself_returnsAPdfDocument() throws Exception {
    byte[] pdf =
        mockMvc
            .perform(get("/api/cards/{id}/pdf", MARIA_ID).with(authAs(MARIA_EMAIL)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(
                header().string("Content-Disposition", "attachment; filename=\"carteirinha.pdf\""))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("%PDF-");
  }

  @Test
  void cardPdf_asPedro_ofMaria_returns404() throws Exception {
    mockMvc
        .perform(get("/api/cards/{id}/pdf", MARIA_ID).with(authAs(PEDRO_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"));
  }

  @Test
  void cardPdf_ofInactiveBeneficiary_returns409() throws Exception {
    mockMvc
        .perform(get("/api/cards/{id}/pdf", INACTIVE_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("card.unavailable"));
  }

  private long countAuditEvents() {
    return jdbc.queryForObject(
        "select count(*) from audit_event where event_type = 'card.dependent-viewed'", Long.class);
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
