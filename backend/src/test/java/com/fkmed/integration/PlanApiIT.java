package com.fkmed.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fkmed.infra.security.TokenClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** SPEC-0001 AC2 (BR3) and the API half of AC3 (BR4/BR5/BR6). */
class PlanApiIT extends AbstractIntegrationTest {

  private static final String MARIA_CARD = "001234567";
  private static final String PEDRO_CARD = "001234575";

  @Autowired private MockMvc mockMvc;

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
  void myPlan_withUnknownCard_returns404PlanNotFound() throws Exception {
    mockMvc
        .perform(get("/api/plan/my-plan").with(cardOf("999999999")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("plan.not-found"));
  }

  @Test
  void myPlan_asMaria_returnsSeededPlanAndFamily() throws Exception {
    mockMvc
        .perform(get("/api/plan/my-plan").with(cardOf(MARIA_CARD)))
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
        .perform(get("/api/plan/my-plan").with(cardOf(PEDRO_CARD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.members.length()").value(2))
        .andExpect(jsonPath("$.members[0].cardNumber").value(MARIA_CARD))
        .andExpect(jsonPath("$.members[1].cardNumber").value(PEDRO_CARD));
  }

  private static RequestPostProcessor cardOf(String cardNumber) {
    return jwt().jwt(jwt -> jwt.claim(TokenClaims.BENEFICIARY_CARD, cardNumber));
  }
}
