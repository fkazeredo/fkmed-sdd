package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0012 API contract over MockMvc and Testcontainers Postgres: the V23 seed (MARIA — em
 * análise/autorizada/negada; PEDRO — none, AC1/AC2), the filtered list (BR2), the type-specific
 * detail (password+validity when authorized, reason when denied — AC3), family scope (SPEC-0003
 * BR3) and the {@code guide.not-found} 404 that never reveals existence. Read-only: the seed rows
 * are immutable from this test's perspective (no cleanup needed — see {@code OperatorSimGuidesIT}
 * for the mutating sim scenarios, which restore what they change).
 */
class GuideApiIT extends AbstractIntegrationTest {

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final UUID MARIA_ID = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");
  private static final String PEDRO_EMAIL = PedroAccountFixture.PEDRO_EMAIL;

  private static final UUID EM_ANALISE_ID = UUID.fromString("ee100000-0000-4000-8000-000000000001");
  private static final UUID AUTORIZADA_ID = UUID.fromString("ee100000-0000-4000-8000-000000000002");
  private static final UUID NEGADA_ID = UUID.fromString("ee100000-0000-4000-8000-000000000003");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  // --- list ---

  @Test
  void list_withoutAuthentication_returns401() throws Exception {
    mockMvc
        .perform(get("/api/guides").param("beneficiaryId", MARIA_ID.toString()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void list_asMaria_returnsThreeGuidesWithDistinctStatuses_mostRecentFirst_ac1() throws Exception {
    String body =
        mockMvc
            .perform(
                get("/api/guides")
                    .param("beneficiaryId", MARIA_ID.toString())
                    .with(authAs(MARIA_EMAIL)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();

    List<String> statuses = com.jayway.jsonpath.JsonPath.read(body, "$[*].status");
    assertThat(statuses).containsExactlyInAnyOrder("EM_ANALISE", "AUTORIZADA", "NEGADA");
    // Most-recent-first: EM_ANALISE (5 days ago) sorts before AUTORIZADA (20 days ago) before
    // NEGADA (35 days ago).
    List<String> ids = com.jayway.jsonpath.JsonPath.read(body, "$[*].id");
    assertThat(ids.indexOf(EM_ANALISE_ID.toString()))
        .isLessThan(ids.indexOf(AUTORIZADA_ID.toString()));
    assertThat(ids.indexOf(AUTORIZADA_ID.toString())).isLessThan(ids.indexOf(NEGADA_ID.toString()));
  }

  @Test
  void list_asPedro_returnsEmptyArray_ac2() throws Exception {
    PedroAccountFixture.seed(jdbc);
    try {
      String pedroId = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";
      mockMvc
          .perform(get("/api/guides").param("beneficiaryId", pedroId).with(authAs(PEDRO_EMAIL)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    } finally {
      PedroAccountFixture.remove(jdbc);
    }
  }

  @Test
  void list_filteredByStatus_returnsOnlyThatStatus() throws Exception {
    mockMvc
        .perform(
            get("/api/guides")
                .param("beneficiaryId", MARIA_ID.toString())
                .param("status", "NEGADA")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(NEGADA_ID.toString()));
  }

  @Test
  void list_periodLast30Days_excludesTheOlderGuide() throws Exception {
    mockMvc
        .perform(
            get("/api/guides")
                .param("beneficiaryId", MARIA_ID.toString())
                .param("period", "LAST_30D")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='" + EM_ANALISE_ID + "')]").exists())
        .andExpect(jsonPath("$[?(@.id=='" + NEGADA_ID + "')]").doesNotExist());
  }

  @Test
  void list_periodLast90Days_includesEveryGuide() throws Exception {
    mockMvc
        .perform(
            get("/api/guides")
                .param("beneficiaryId", MARIA_ID.toString())
                .param("period", "LAST_90D")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  void list_outOfScopeBeneficiary_returns404_withoutRevealingExistence() throws Exception {
    mockMvc
        .perform(
            get("/api/guides")
                .param("beneficiaryId", UUID.randomUUID().toString())
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"));
  }

  @Test
  void list_asPedro_neverSeesMariasGuides() throws Exception {
    PedroAccountFixture.seed(jdbc);
    try {
      mockMvc
          .perform(
              get("/api/guides")
                  .param("beneficiaryId", MARIA_ID.toString())
                  .with(authAs(PEDRO_EMAIL)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"));
    } finally {
      PedroAccountFixture.remove(jdbc);
    }
  }

  // --- detail ---

  @Test
  void detail_authorizedGuide_showsPasswordAndValidity_ac3() throws Exception {
    mockMvc
        .perform(
            get("/api/guides/{id}", AUTORIZADA_ID)
                .param("beneficiaryId", MARIA_ID.toString())
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("AUTORIZADA"))
        .andExpect(jsonPath("$.authPassword").value("AUT-482913"))
        .andExpect(jsonPath("$.authValidUntil").isNotEmpty())
        .andExpect(jsonPath("$.authExpired").value(false))
        .andExpect(jsonPath("$.denialReason").doesNotExist())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].status").value("AUTORIZADO"));
  }

  @Test
  void detail_deniedGuide_showsReason_ac3() throws Exception {
    mockMvc
        .perform(
            get("/api/guides/{id}", NEGADA_ID)
                .param("beneficiaryId", MARIA_ID.toString())
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NEGADA"))
        .andExpect(jsonPath("$.denialReason").value("Documentação insuficiente"))
        .andExpect(jsonPath("$.authPassword").doesNotExist())
        .andExpect(jsonPath("$.items[0].status").value("NEGADO"));
  }

  @Test
  void detail_emAnaliseGuide_hidesPasswordAndReason() throws Exception {
    mockMvc
        .perform(
            get("/api/guides/{id}", EM_ANALISE_ID)
                .param("beneficiaryId", MARIA_ID.toString())
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EM_ANALISE"))
        .andExpect(jsonPath("$.authPassword").doesNotExist())
        .andExpect(jsonPath("$.denialReason").doesNotExist())
        .andExpect(jsonPath("$.items[0].status").value("EM_ANALISE"));
  }

  @Test
  void detail_ofUnknownId_returns404_guideNotFound() throws Exception {
    mockMvc
        .perform(
            get("/api/guides/{id}", "00000000-0000-0000-0000-000000000000")
                .param("beneficiaryId", MARIA_ID.toString())
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("guide.not-found"));
  }

  @Test
  void detail_ofOutOfScopeBeneficiary_returns404_beforeRevealingTheGuide() throws Exception {
    mockMvc
        .perform(
            get("/api/guides/{id}", AUTORIZADA_ID)
                .param("beneficiaryId", UUID.randomUUID().toString())
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"));
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
