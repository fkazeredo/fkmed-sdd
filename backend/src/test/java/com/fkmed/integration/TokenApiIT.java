package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0012 BR9-BR12 API contract over MockMvc and Testcontainers Postgres: token generation (6
 * digits, +10min expiry), single-validity (a new token invalidates the previous — AC5), {@code
 * token.none-active} when there is none, family scope (SPEC-0003 BR3) and the BR12 dependent-
 * authorship audit. Cleaned in {@code @BeforeEach}/{@code @AfterEach} — absolute-count assertions
 * on the shared {@code attendance_token}/{@code audit_event} tables need real isolation.
 */
class TokenApiIT extends AbstractIntegrationTest {

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final String MARIA_ACCOUNT_ID = "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70";
  private static final String PEDRO_EMAIL = PedroAccountFixture.PEDRO_EMAIL;
  private static final String PEDRO_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update(
        "delete from attendance_token where beneficiary_id in (?::uuid, ?::uuid)",
        MARIA_ID,
        PEDRO_ID);
    jdbc.update(
        "delete from audit_event where event_type = 'guides.dependent-token-generated'"
            + " and target_beneficiary_id = ?::uuid",
        PEDRO_ID);
  }

  @Test
  void generate_withoutAuthentication_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void generate_forSelf_returns201WithSixDigitsAndTenMinuteExpiry_ac4() throws Exception {
    mockMvc
        .perform(
            post("/api/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").isNotEmpty())
        .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("\\d{6}")))
        .andExpect(jsonPath("$.expiresAt").isNotEmpty());
  }

  @Test
  void current_whenNoneGenerated_returns404_tokenNoneActive() throws Exception {
    mockMvc
        .perform(
            get("/api/tokens/current").param("beneficiaryId", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("token.none-active"));
  }

  @Test
  void current_afterGenerate_returnsTheSameCode() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}")
                    .with(authAs(MARIA_EMAIL)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String generatedCode = com.jayway.jsonpath.JsonPath.read(body, "$.code");

    mockMvc
        .perform(
            get("/api/tokens/current").param("beneficiaryId", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(generatedCode));
  }

  @Test
  void generate_again_invalidatesThePreviousToken_ac5() throws Exception {
    String firstBody =
        mockMvc
            .perform(
                post("/api/tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}")
                    .with(authAs(MARIA_EMAIL)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String firstCode = com.jayway.jsonpath.JsonPath.read(firstBody, "$.code");

    String secondBody =
        mockMvc
            .perform(
                post("/api/tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}")
                    .with(authAs(MARIA_EMAIL)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String secondCode = com.jayway.jsonpath.JsonPath.read(secondBody, "$.code");

    // Only the newest token is active; the previous one is invalidated (never shown as valid).
    mockMvc
        .perform(
            get("/api/tokens/current").param("beneficiaryId", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(secondCode));

    Long activeCount =
        jdbc.queryForObject(
            "select count(*) from attendance_token where beneficiary_id = ?::uuid"
                + " and invalidated_at is null",
            Long.class,
            MARIA_ID);
    assertThat(activeCount).isEqualTo(1);
    Long invalidatedCount =
        jdbc.queryForObject(
            "select count(*) from attendance_token where beneficiary_id = ?::uuid"
                + " and code = ? and invalidated_at is not null",
            Long.class,
            MARIA_ID,
            firstCode);
    assertThat(invalidatedCount).isEqualTo(1);
  }

  @Test
  void generate_forAnOutOfScopeBeneficiary_returns404() throws Exception {
    mockMvc
        .perform(
            post("/api/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + UUID.randomUUID() + "\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"));
  }

  @Test
  void generate_forSelf_doesNotAudit() throws Exception {
    long before = countDependentTokenAudits();

    mockMvc
        .perform(
            post("/api/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isCreated());

    assertThat(countDependentTokenAudits()).isEqualTo(before);
  }

  @Test
  void generate_byTitularForDependent_recordsTheAuthorshipAudit_br12() throws Exception {
    PedroAccountFixture.seed(jdbc);
    try {
      long before = countDependentTokenAudits();

      mockMvc
          .perform(
              post("/api/tokens")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"beneficiaryId\":\"" + PEDRO_ID + "\"}")
                  .with(authAs(MARIA_EMAIL)))
          .andExpect(status().isCreated());

      assertThat(countDependentTokenAudits()).isEqualTo(before + 1);
      var row =
          jdbc.queryForMap(
              "select * from audit_event where event_type = 'guides.dependent-token-generated'"
                  + " order by occurred_at desc limit 1");
      assertThat(row.get("author_account_id").toString()).isEqualTo(MARIA_ACCOUNT_ID);
      assertThat(row.get("target_beneficiary_id").toString()).isEqualTo(PEDRO_ID);
    } finally {
      PedroAccountFixture.remove(jdbc);
    }
  }

  private long countDependentTokenAudits() {
    return jdbc.queryForObject(
        "select count(*) from audit_event where event_type = 'guides.dependent-token-generated'"
            + " and target_beneficiary_id = ?::uuid",
        Long.class,
        PEDRO_ID);
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
