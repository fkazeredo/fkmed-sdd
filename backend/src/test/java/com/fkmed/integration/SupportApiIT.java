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
 * SPEC-0014 API contract over MockMvc and Testcontainers Postgres: channel cards (BR1), antifraud
 * content (BR3), FAQ search (BR5/BR6, AC1) and Libras service-request registration (BR4, AC4) —
 * family scope (SPEC-0003 BR3) and the always-on audit entry. The exact inside/outside-hours branch
 * of BR4 is unit-tested with a fixed {@code Clock} in {@code SupportServiceTest}; this IT proves
 * the wiring (persistence + audit + HTTP contract) without depending on wall-clock time, to avoid
 * the flakiness of asserting a specific hours branch against the real clock.
 */
class SupportApiIT extends AbstractIntegrationTest {

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
        "delete from libras_request where beneficiary_id in (?::uuid, ?::uuid)",
        MARIA_ID,
        PEDRO_ID);
    jdbc.update(
        "delete from audit_event where event_type = 'support.libras-requested'"
            + " and target_beneficiary_id in (?::uuid, ?::uuid)",
        MARIA_ID,
        PEDRO_ID);
  }

  @Test
  void channels_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(get("/api/support/channels")).andExpect(status().isUnauthorized());
  }

  @Test
  void channels_returnsTheBr1SeedInOrder() throws Exception {
    mockMvc
        .perform(get("/api/support/channels").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5))
        .andExpect(jsonPath("$[0].type").value("CENTRAL"))
        .andExpect(jsonPath("$[0].sublabel").value("Capitais"))
        .andExpect(jsonPath("$[1].type").value("CENTRAL"))
        .andExpect(jsonPath("$[1].sublabel").value("Demais localidades"))
        .andExpect(jsonPath("$[2].type").value("WHATSAPP"))
        .andExpect(jsonPath("$[3].type").value("OUVIDORIA"))
        .andExpect(jsonPath("$[4].type").value("ANS"));
  }

  @Test
  void antifraud_returnsTheBr3SeedContent() throws Exception {
    mockMvc
        .perform(get("/api/support/antifraud").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Alerta de golpe!"))
        .andExpect(
            jsonPath("$.message")
                .value("A operadora não solicita dados ou pagamentos por WhatsApp."));
  }

  @Test
  void faq_withoutFilters_returnsAtLeastTwelveEntriesAcrossSixCategories() throws Exception {
    mockMvc
        .perform(get("/api/support/faq").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(12)));
  }

  @Test
  void faq_searchingReembolso_returnsOnlyReembolsoRelatedQuestions_ac1() throws Exception {
    String body =
        mockMvc
            .perform(get("/api/support/faq").param("q", "reembolso").with(jwt()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    java.util.List<String> categories = com.jayway.jsonpath.JsonPath.read(body, "$[*].category");
    assertThat(categories).isNotEmpty().allMatch("REEMBOLSO"::equals);
  }

  @Test
  void faq_clearingTheSearch_restoresTheFullList_ac1() throws Exception {
    mockMvc
        .perform(get("/api/support/faq").param("q", "").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(12)));
  }

  @Test
  void faq_withCategoryFilter_restrictsToCategory() throws Exception {
    mockMvc
        .perform(get("/api/support/faq").param("category", "REDE").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$[*].category",
                org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("REDE"))));
  }

  @Test
  void librasRequest_withoutAuthentication_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/support/libras-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void librasRequest_forSelf_registersAndConfirmsANextStep_ac4() throws Exception {
    mockMvc
        .perform(
            post("/api/support/libras-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.situation").value("REGISTERED"))
        .andExpect(
            jsonPath("$.nextStep")
                .value(
                    org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is("videocall-shortly"),
                        org.hamcrest.Matchers.is("next-period"))));

    Long persisted =
        jdbc.queryForObject(
            "select count(*) from libras_request where beneficiary_id = ?::uuid",
            Long.class,
            MARIA_ID);
    assertThat(persisted).isEqualTo(1);
  }

  @Test
  void librasRequest_forAnyBeneficiary_recordsTheAudit() throws Exception {
    PedroAccountFixture.seed(jdbc);
    try {
      mockMvc
          .perform(
              post("/api/support/libras-requests")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"beneficiaryId\":\"" + PEDRO_ID + "\"}")
                  .with(authAs(MARIA_EMAIL)))
          .andExpect(status().isCreated());

      var row =
          jdbc.queryForMap(
              "select * from audit_event where event_type = 'support.libras-requested'"
                  + " order by occurred_at desc limit 1");
      assertThat(row.get("author_account_id").toString()).isEqualTo(MARIA_ACCOUNT_ID);
      assertThat(row.get("target_beneficiary_id").toString()).isEqualTo(PEDRO_ID);
    } finally {
      PedroAccountFixture.remove(jdbc);
    }
  }

  @Test
  void librasRequest_forAnOutOfScopeBeneficiary_returns404() throws Exception {
    mockMvc
        .perform(
            post("/api/support/libras-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + UUID.randomUUID() + "\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"));
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
