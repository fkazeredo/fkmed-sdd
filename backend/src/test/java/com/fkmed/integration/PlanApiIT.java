package com.fkmed.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * SPEC-0001 AC2 (BR3) and the API half of AC3 (BR4/BR5/BR6). The caller's beneficiary card is
 * resolved server-side from the authenticated account (ADR-0009): callers authenticate as the
 * seeded login e-mail (MARIA is seeded by Flyway V3; PEDRO's account is a disposable fixture
 * created here), never by injecting a card claim.
 */
class PlanApiIT extends AbstractIntegrationTest {

  private static final String MARIA_CARD = "001234567";
  private static final String PEDRO_CARD = "001234575";
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String PEDRO_EMAIL = PedroAccountFixture.PEDRO_EMAIL;

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void seedPedroAccount() {
    // PEDRO (dependent) has no Flyway-seeded account; a disposable one lets server-side card
    // resolution (ADR-0009) yield PEDRO_CARD. Isolation (docs/architecture/testing.md): seed in
    // @BeforeEach, not only @AfterEach.
    PedroAccountFixture.seed(jdbc);
  }

  @AfterEach
  void removePedroAccount() {
    PedroAccountFixture.remove(jdbc);
  }

  @Test
  void myPlan_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(get("/api/plan/my-plan")).andExpect(status().isUnauthorized());
  }

  @Test
  void unknownApiRoute_withoutAuthentication_returns401_withoutLeakingRouteExistence()
      throws Exception {
    mockMvc.perform(get("/api/does-not-exist")).andExpect(status().isUnauthorized());
  }

  @Test
  void myPlan_authenticatedWithoutBeneficiaryLink_returns404PlanNotFound() throws Exception {
    mockMvc
        .perform(get("/api/plan/my-plan").with(jwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("plan.not-found"))
        .andExpect(
            jsonPath("$.message").value("Nenhum plano vinculado ao seu usuário foi encontrado."));
  }

  @Test
  void myPlan_authenticatedAsAccountWithNoBeneficiary_returns404PlanNotFound() throws Exception {
    // ADR-0009: the card can no longer be forged via a claim — a principal that maps to no
    // beneficiary simply resolves to no card, so no plan is disclosed (the former "unknown card"
    // path, now expressed as an unmapped authenticated principal).
    mockMvc
        .perform(get("/api/plan/my-plan").with(authAs("desconhecido@fkmed.local")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("plan.not-found"));
  }

  @Test
  void myPlan_asMaria_returnsSeededPlanAndFamily() throws Exception {
    mockMvc
        .perform(get("/api/plan/my-plan").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plan.name").value("PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP"))
        .andExpect(jsonPath("$.plan.ansRegistration").value("326305"))
        .andExpect(jsonPath("$.plan.coverage").value("ESTADUAL"))
        .andExpect(jsonPath("$.plan.copay").value(true))
        .andExpect(jsonPath("$.plan.reimbursement").value(true))
        .andExpect(jsonPath("$.plan.additives[0]").value("Urg/emerg Nacional Hr — Assistência"))
        .andExpect(jsonPath("$.plan.additives.length()").value(1))
        .andExpect(jsonPath("$.members.length()").value(2))
        .andExpect(jsonPath("$.members[0].fullName").value("MARIA CLARA SOUZA LIMA"))
        .andExpect(jsonPath("$.members[0].role").value("TITULAR"))
        .andExpect(jsonPath("$.members[0].cardNumber").value(MARIA_CARD))
        .andExpect(jsonPath("$.members[1].fullName").value("PEDRO SOUZA LIMA"))
        .andExpect(jsonPath("$.members[1].role").value("DEPENDENT"))
        .andExpect(jsonPath("$.members[1].cardNumber").value(PEDRO_CARD));
  }

  @Test
  void myPlan_asPedro_resolvesTheSameFamilyThroughTheTitular() throws Exception {
    mockMvc
        .perform(get("/api/plan/my-plan").with(authAs(PEDRO_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.members.length()").value(2))
        .andExpect(jsonPath("$.members[0].cardNumber").value(MARIA_CARD))
        .andExpect(jsonPath("$.members[1].cardNumber").value(PEDRO_CARD));
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
