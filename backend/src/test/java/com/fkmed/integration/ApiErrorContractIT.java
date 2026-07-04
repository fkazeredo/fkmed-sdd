package com.fkmed.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regression for review finding I1: the global handler's catch-all must not swallow framework
 * exceptions into 500 — an authenticated call to an unknown route is a 404 and a wrong method is a
 * 405, each with its proper status (baseline §0011: predictable error contract).
 */
class ApiErrorContractIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void unknownRoute_authenticated_returns404_not500() throws Exception {
    mockMvc
        .perform(get("/api/unknown-route").with(jwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("http.404"));
  }

  @Test
  void wrongMethod_onExistingRoute_returns405_not500() throws Exception {
    mockMvc
        .perform(post("/api/plan/my-plan").with(jwt()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("http.405"));
  }
}
