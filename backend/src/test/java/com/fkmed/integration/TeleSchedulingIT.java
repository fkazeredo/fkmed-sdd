package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
 * SPEC-0010 BR14 × SPEC-0009 (Phase-4 Wave-2 reconciliation): the scheduled-teleconsultation scope
 * the merged FE consumes — availability and booking with {@code telemedicine=true} and NO {@code
 * unitId} (the virtual Telemedicina unit is resolved server-side, DL-0018), the booking recorded as
 * {@code TELEMEDICINA}, and Meus Agendamentos filtered by {@code telemedicine=true}. Tele bookings
 * are cleaned for isolation on the shared Postgres.
 */
class TeleSchedulingIT extends AbstractIntegrationTest {

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final UUID MARIA_ID = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");
  private static final UUID TELE_UNIT = UUID.fromString("dd000000-0000-4000-8000-000000000001");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void cleanTeleBookings() {
    jdbc.update(
        "delete from appointment where beneficiary_id = ?::uuid and unit_id = ?::uuid",
        MARIA_ID,
        TELE_UNIT);
    jdbc.update(
        "update schedule_slot set occupied = 0 where agenda_id in"
            + " (select id from unit_agenda where unit_id = ?::uuid)",
        TELE_UNIT);
  }

  @Test
  void booksTeleConsultation_withoutUnitId_recordsTelemedicineModality_andListsUnderTeleScope()
      throws Exception {
    // Availability with the telemedicine scope and no unitId (BR14): the virtual unit is resolved
    // server-side; take its first bookable slot.
    String availability =
        mockMvc
            .perform(
                get("/api/appointments/availability")
                    .param("specialty", "CLINICA_MEDICA")
                    .param("telemedicine", "true")
                    .with(authAs(MARIA_EMAIL)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String slot = JsonPath.read(availability, "$[0].slots[0].slot");

    // Book WITHOUT a unitId, telemedicine=true.
    mockMvc
        .perform(
            post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"beneficiaryId\":\""
                        + MARIA_ID
                        + "\",\"type\":\"CONSULTATION\",\"specialty\":\"CLINICA_MEDICA\","
                        + "\"telemedicine\":true,\"slot\":\""
                        + slot
                        + "\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.protocol").isNotEmpty())
        .andExpect(jsonPath("$.status").value("AGENDADO"));

    // Recorded as TELEMEDICINA on the virtual unit.
    assertThat(
            jdbc.queryForObject(
                "select modality from appointment where beneficiary_id = ?::uuid"
                    + " and unit_id = ?::uuid",
                String.class,
                MARIA_ID,
                TELE_UNIT))
        .isEqualTo("TELEMEDICINA");

    // Meus Agendamentos filtered by the tele scope returns it.
    mockMvc
        .perform(get("/api/appointments").param("telemedicine", "true").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.upcoming[?(@.modality=='TELEMEDICINA')]").exists())
        .andExpect(jsonPath("$.upcoming[?(@.modality=='PRESENCIAL')]").doesNotExist());
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
