package com.fkmed.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * SPEC-0018 BR1/AC-flag-off: with {@code app.sim.enabled=false} the whole {@code /api/sim/**}
 * family is ABSENT (404), not merely forbidden — the controller is {@code @ConditionalOnProperty}
 * so it is not registered. Proven with an AUTHENTICATED operator so the 404 is the route's absence,
 * not a 401. Its own Spring context (the property overrides the dev default) reuses the shared
 * Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.sim.enabled=false")
class OperatorSimDisabledIT {

  @ServiceConnection static final PostgreSQLContainer POSTGRES = SharedPostgres.INSTANCE;

  @Autowired private MockMvc mockMvc;

  @Test
  void simRoutes_areAbsent_whenTheFlagIsOff() throws Exception {
    mockMvc
        .perform(
            post("/api/sim/tele/sessions/next/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professionalName\":\"Dra. Ana\",\"crm\":\"CRM-RJ 1\"}")
                .with(operator()))
        .andExpect(status().isNotFound());
  }

  private static RequestPostProcessor operator() {
    return jwt().jwt(jwt -> jwt.subject("operador-sim@fkmed.local"));
  }
}
