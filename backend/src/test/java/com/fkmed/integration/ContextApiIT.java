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

/**
 * SPEC-0003 context & family-scope authorization (BR1-BR5, BR2/BR3; AC1/AC2). MARIA (titular) may
 * act for herself and PEDRO; PEDRO (dependent) only for himself and is denied MARIA's data with a
 * not-found that never reveals her existence. Read-only against the canonical seed — no rows are
 * mutated, so no absolute-count isolation is needed.
 */
class ContextApiIT extends AbstractIntegrationTest {

  private static final String MARIA_CARD = "001234567";
  private static final String PEDRO_CARD = "001234575";
  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final String PEDRO_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";
  private static final String PLAN_NAME = "PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP";

  @Autowired private MockMvc mockMvc;

  @Test
  void accessibleBeneficiaries_withoutAuthentication_returns401() throws Exception {
    mockMvc
        .perform(get("/api/context/accessible-beneficiaries"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void accessibleBeneficiaries_asMaria_returnsHerselfThenPedro() throws Exception {
    mockMvc
        .perform(get("/api/context/accessible-beneficiaries").with(cardOf(MARIA_CARD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].beneficiaryId").value(MARIA_ID))
        .andExpect(jsonPath("$[0].firstName").value("MARIA"))
        .andExpect(jsonPath("$[0].role").value("TITULAR"))
        .andExpect(jsonPath("$[1].beneficiaryId").value(PEDRO_ID))
        .andExpect(jsonPath("$[1].firstName").value("PEDRO"))
        .andExpect(jsonPath("$[1].role").value("DEPENDENT"));
  }

  @Test
  void accessibleBeneficiaries_asPedro_returnsOnlyHimself() throws Exception {
    mockMvc
        .perform(get("/api/context/accessible-beneficiaries").with(cardOf(PEDRO_CARD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].beneficiaryId").value(PEDRO_ID))
        .andExpect(jsonPath("$[0].firstName").value("PEDRO"))
        .andExpect(jsonPath("$[0].role").value("DEPENDENT"));
  }

  @Test
  void beneficiary_asMaria_ofPedro_returnsScopedSummary() throws Exception {
    mockMvc
        .perform(get("/api/context/beneficiaries/{id}", PEDRO_ID).with(cardOf(MARIA_CARD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.beneficiaryId").value(PEDRO_ID))
        .andExpect(jsonPath("$.firstName").value("PEDRO"))
        .andExpect(jsonPath("$.fullName").value("PEDRO SOUZA LIMA"))
        .andExpect(jsonPath("$.role").value("DEPENDENT"))
        .andExpect(jsonPath("$.planName").value(PLAN_NAME))
        .andExpect(jsonPath("$.cardNumber").value(PEDRO_CARD));
  }

  @Test
  void beneficiary_asMaria_ofHerself_returnsSummary() throws Exception {
    mockMvc
        .perform(get("/api/context/beneficiaries/{id}", MARIA_ID).with(cardOf(MARIA_CARD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.beneficiaryId").value(MARIA_ID))
        .andExpect(jsonPath("$.firstName").value("MARIA"))
        .andExpect(jsonPath("$.role").value("TITULAR"))
        .andExpect(jsonPath("$.cardNumber").value(MARIA_CARD));
  }

  @Test
  void beneficiary_asPedro_ofMaria_returns404WithoutRevealingExistence() throws Exception {
    mockMvc
        .perform(get("/api/context/beneficiaries/{id}", MARIA_ID).with(cardOf(PEDRO_CARD)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"))
        .andExpect(jsonPath("$.message").value("Beneficiário não encontrado."));
  }

  @Test
  void beneficiary_ofUnknownId_asMaria_returns404() throws Exception {
    mockMvc
        .perform(
            get("/api/context/beneficiaries/{id}", "00000000-0000-0000-0000-000000000000")
                .with(cardOf(MARIA_CARD)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"));
  }

  private static RequestPostProcessor cardOf(String cardNumber) {
    return jwt().jwt(jwt -> jwt.claim(TokenClaims.BENEFICIARY_CARD, cardNumber));
  }
}
