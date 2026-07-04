package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.web.servlet.MockMvc;

/** SPEC-0001 AC1 (BR1) and AC4 (BR2): public system endpoints. */
class SystemEndpointsIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private BuildProperties buildProperties;

  @Test
  void health_isPublic_andReportsApplicationAndDatabaseUp() throws Exception {
    mockMvc
        .perform(get("/api/system/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.database").value("UP"));
  }

  @Test
  void version_isPublic_andMatchesBuildConfiguration_neverHardcoded() throws Exception {
    String body =
        mockMvc
            .perform(get("/api/system/version"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String version = JsonPath.read(body, "$.version");
    String commit = JsonPath.read(body, "$.commit");
    assertThat(version).isEqualTo(buildProperties.getVersion());
    assertThat(commit).isNotBlank().isNotEqualTo("unknown").matches("[0-9a-f]{7,40}");
  }
}
