package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * SPEC-0014 API contract over MockMvc and Testcontainers Postgres: channel cards (BR1/BR2),
 * antifraud copy (BR3), the FAQ (BR5/BR6, case/accent-insensitive over question + answer) and the
 * Central de Libras request (BR4) — scope, persistence, the unconditional audit and the operating
 * -hours-driven next step, using a deterministic {@link MutableClock} (SPEC-0003 BR6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(SupportApiIT.FixedClockConfig.class)
class SupportApiIT {

  @ServiceConnection static final PostgreSQLContainer POSTGRES = SharedPostgres.INSTANCE;

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final String MARIA_ACCOUNT_ID = "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70";

  static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  // Wednesday 2026-07-08, 10:00 — inside the 8h-18h weekday operating window.
  static final Instant WITHIN_HOURS = ZonedDateTime.of(2026, 7, 8, 10, 0, 0, 0, ZONE).toInstant();
  // Same Wednesday, 20:00 — outside the window.
  static final Instant OUTSIDE_HOURS = ZonedDateTime.of(2026, 7, 8, 20, 0, 0, 0, ZONE).toInstant();

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MutableClock clock;

  @BeforeEach
  @AfterEach
  void clean() {
    clock.reset(WITHIN_HOURS);
    jdbc.update("delete from libras_request where beneficiary_id = ?::uuid", MARIA_ID);
    jdbc.update(
        "delete from audit_event where event_type = 'support.libras-request-registered'"
            + " and target_beneficiary_id = ?::uuid",
        MARIA_ID);
  }

  // --- Channels (BR1/BR2) ---

  @Test
  void channels_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(get("/api/support/channels")).andExpect(status().isUnauthorized());
  }

  @Test
  void channels_returnsTheSeedInOrder_br1() throws Exception {
    mockMvc
        .perform(get("/api/support/channels").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[0].type").value("CENTRAL"))
        .andExpect(jsonPath("$[0].label").value("Central de Atendimento 24h"))
        .andExpect(jsonPath("$[0].displayOrder").value(1))
        .andExpect(jsonPath("$[1].type").value("WHATSAPP"))
        .andExpect(jsonPath("$[2].type").value("OUVIDORIA"))
        .andExpect(jsonPath("$[3].type").value("ANS"));
  }

  // --- Antifraud (BR3) ---

  @Test
  void antifraud_returnsTheFixedCopy_br3() throws Exception {
    mockMvc
        .perform(get("/api/support/antifraud").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Alerta de golpe!"))
        .andExpect(
            jsonPath("$.message")
                .value("A operadora não solicita dados ou pagamentos por WhatsApp"))
        .andExpect(jsonPath("$.bestPractices.length()").value(3));
  }

  // --- FAQ (BR5/BR6) ---

  @Test
  void faq_withoutFilters_returnsTheFullSeed_br6() throws Exception {
    mockMvc
        .perform(get("/api/support/faq").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(12)));
  }

  @Test
  void faq_filteredByReembolsoTerm_returnsOnlyReimbursementRelated_ac1() throws Exception {
    mockMvc
        .perform(get("/api/support/faq").param("q", "reembolso").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
        .andExpect(
            jsonPath("$[*].category")
                .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("REEMBOLSO"))));
  }

  @Test
  void faq_searchIsCaseAndAccentInsensitive() throws Exception {
    mockMvc
        .perform(get("/api/support/faq").param("q", "REEMBOLSO").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));

    mockMvc
        .perform(get("/api/support/faq").param("q", "nao representa").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
  }

  @Test
  void faq_filteredByCategory_carteirinha() throws Exception {
    mockMvc
        .perform(get("/api/support/faq").param("category", "CARTEIRINHA").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(
            jsonPath("$[*].category")
                .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("CARTEIRINHA"))));
  }

  @Test
  void faq_noMatches_returnsEmptyList() throws Exception {
    mockMvc
        .perform(get("/api/support/faq").param("q", "xyzxyznaoexiste").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // --- Central de Libras (BR4) ---

  @Test
  void libras_withoutAuthentication_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/support/libras-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void libras_withinOperatingHours_returnsVideocallShortly_ac4() throws Exception {
    clock.reset(WITHIN_HOURS);

    mockMvc
        .perform(
            post("/api/support/libras-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.situation").value("REGISTERED"))
        .andExpect(jsonPath("$.nextStep").value("videocall-shortly"));
  }

  @Test
  void libras_outsideOperatingHours_returnsNextPeriodWithHours_ac4() throws Exception {
    clock.reset(OUTSIDE_HOURS);

    mockMvc
        .perform(
            post("/api/support/libras-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.situation").value("REGISTERED"))
        .andExpect(jsonPath("$.nextStep").value("next-period"))
        .andExpect(jsonPath("$.hours").value("Segunda a sexta, das 8h às 18h"));
  }

  @Test
  void libras_persistsTheRequestAsRegistered() throws Exception {
    clock.reset(WITHIN_HOURS);

    mockMvc
        .perform(
            post("/api/support/libras-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isCreated());

    Long count =
        jdbc.queryForObject(
            "select count(*) from libras_request where beneficiary_id = ?::uuid"
                + " and situation = 'REGISTERED'",
            Long.class,
            MARIA_ID);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void libras_auditsTheRegistration() throws Exception {
    clock.reset(WITHIN_HOURS);

    mockMvc
        .perform(
            post("/api/support/libras-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"" + MARIA_ID + "\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isCreated());

    var row =
        jdbc.queryForMap(
            "select * from audit_event where event_type = 'support.libras-request-registered'"
                + " and target_beneficiary_id = ?::uuid",
            MARIA_ID);
    assertThat(row.get("author_account_id").toString()).isEqualTo(MARIA_ACCOUNT_ID);
  }

  @Test
  void libras_forAnOutOfScopeBeneficiary_returns404() throws Exception {
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

  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(WITHIN_HOURS, ZONE);
    }
  }
}
