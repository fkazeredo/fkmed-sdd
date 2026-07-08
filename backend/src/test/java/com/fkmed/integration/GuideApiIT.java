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
 * BR3) and the {@code guide.not-found} 404 that never reveals existence. Read-only over the V23
 * seed; the list assertions are **seed-focused** (they identify the three seed guides by id, not by
 * an absolute {@code $.length()} over MARIA's guides) so a sibling IT leaving an extra guide on
 * MARIA in the shared Postgres cannot break them — the root cause of the CI-only flaky {@code
 * debt_guideapiit_flaky_isolation}, whose real fix is {@code OperatorSimGuidesIT} now cleaning the
 * guides it creates. The regression test below proves that robustness deterministically.
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
  void list_asMaria_returnsTheThreeSeedGuidesWithDistinctStatuses_mostRecentFirst_ac1()
      throws Exception {
    String body =
        mockMvc
            .perform(
                get("/api/guides")
                    .param("beneficiaryId", MARIA_ID.toString())
                    .with(authAs(MARIA_EMAIL)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Seed-focused (not an absolute count): the three seed guides are present with their distinct
    // statuses, robust to any extra guide a sibling IT may leave on MARIA (the old $.length()==3
    // was the CI-only flaky — debt_guideapiit_flaky_isolation).
    assertThat(statusOf(body, EM_ANALISE_ID)).isEqualTo("EM_ANALISE");
    assertThat(statusOf(body, AUTORIZADA_ID)).isEqualTo("AUTORIZADA");
    assertThat(statusOf(body, NEGADA_ID)).isEqualTo("NEGADA");
    // Most-recent-first among the seed guides: EM_ANALISE (5d) before AUTORIZADA (20d) before
    // NEGADA (35d).
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
    String body =
        mockMvc
            .perform(
                get("/api/guides")
                    .param("beneficiaryId", MARIA_ID.toString())
                    .param("status", "NEGADA")
                    .with(authAs(MARIA_EMAIL)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // The filter returns only NEGADA guides and includes the seed NEGADA one — robust to any extra
    // guide on MARIA (those are EM_ANALISE and excluded by the filter).
    List<String> statuses = com.jayway.jsonpath.JsonPath.read(body, "$[*].status");
    assertThat(statuses).isNotEmpty().containsOnly("NEGADA");
    List<String> ids = com.jayway.jsonpath.JsonPath.read(body, "$[*].id");
    assertThat(ids).contains(NEGADA_ID.toString());
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
    String body =
        mockMvc
            .perform(
                get("/api/guides")
                    .param("beneficiaryId", MARIA_ID.toString())
                    .param("period", "LAST_90D")
                    .with(authAs(MARIA_EMAIL)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // All three seed guides fall within 90 days; assert their presence, not an absolute count.
    List<String> ids = com.jayway.jsonpath.JsonPath.read(body, "$[*].id");
    assertThat(ids)
        .contains(EM_ANALISE_ID.toString(), AUTORIZADA_ID.toString(), NEGADA_ID.toString());
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

  // --- regression: robustness to extra guides on the same beneficiary ---

  @Test
  void list_identifiesTheSeedGuides_evenWithAnotherGuideOnMaria_regression() throws Exception {
    // Deterministically reproduces the CI-only flaky (debt_guideapiit_flaky_isolation): an extra
    // guide on MARIA in the shared Postgres must NOT break the seed-focused list assertions — the
    // old absolute "$.length()==3" failed exactly here (expected:<3> but was:<4>). Manages its own
    // row so it stays isolated (docs/architecture/testing.md).
    UUID extra = UUID.fromString("ee1e0000-0000-4000-8000-0000000000ff");
    jdbc.update(
        "insert into guide (id, number, type, beneficiary_id, requesting_provider, requested_at,"
            + " status) values (?::uuid, 'GD-REG-0001', 'CONSULTA', ?::uuid, 'Regression Provider',"
            + " current_date - 1, 'EM_ANALISE')",
        extra,
        MARIA_ID);
    jdbc.update(
        "insert into guide_item (id, guide_id, tuss_code, description, quantity, status) values"
            + " (?::uuid, ?::uuid, '10101012', 'Regression item', 1, 'EM_ANALISE')",
        UUID.randomUUID(),
        extra);
    try {
      String body =
          mockMvc
              .perform(
                  get("/api/guides")
                      .param("beneficiaryId", MARIA_ID.toString())
                      .with(authAs(MARIA_EMAIL)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      // The three seed guides are still correctly identified despite the extra row.
      assertThat(statusOf(body, EM_ANALISE_ID)).isEqualTo("EM_ANALISE");
      assertThat(statusOf(body, AUTORIZADA_ID)).isEqualTo("AUTORIZADA");
      assertThat(statusOf(body, NEGADA_ID)).isEqualTo("NEGADA");
      List<String> ids = com.jayway.jsonpath.JsonPath.read(body, "$[*].id");
      assertThat(ids).contains(extra.toString());
    } finally {
      jdbc.update("delete from guide_item where guide_id = ?::uuid", extra);
      jdbc.update("delete from guide where id = ?::uuid", extra);
    }
  }

  /** The status of the guide with {@code id} in the list response, or {@code null} when absent. */
  private static String statusOf(String body, UUID id) {
    List<String> statuses =
        com.jayway.jsonpath.JsonPath.read(body, "$[?(@.id=='" + id + "')].status");
    return statuses.isEmpty() ? null : statuses.get(0);
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
