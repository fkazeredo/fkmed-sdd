package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * BOOTSTRAP §2 gate 6: the committed OpenAPI snapshot ({@code docs/api/openapi.json}) is the API
 * contract; any drift fails the build. Regenerate with {@code -Dopenapi.snapshot.write=true}.
 */
class OpenApiSnapshotIT extends AbstractIntegrationTest {

  private static final Path SNAPSHOT = Path.of("..", "docs", "api", "openapi.json");

  @Autowired private MockMvc mockMvc;

  @Test
  void openApiContract_matchesTheCommittedSnapshot() throws Exception {
    String body =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    String pretty =
        JsonMapper.builder().build().readTree(body).toPrettyString().replace("\r\n", "\n") + "\n";

    if (Boolean.getBoolean("openapi.snapshot.write")) {
      Files.createDirectories(SNAPSHOT.getParent());
      Files.writeString(SNAPSHOT, pretty, StandardCharsets.UTF_8);
      return;
    }

    assertThat(SNAPSHOT)
        .as("missing committed OpenAPI snapshot — run ./mvnw verify -Dopenapi.snapshot.write=true")
        .exists();
    String committed = Files.readString(SNAPSHOT, StandardCharsets.UTF_8).replace("\r\n", "\n");
    assertThat(pretty)
        .as(
            "OpenAPI contract drift — review it and regenerate the snapshot with "
                + "-Dopenapi.snapshot.write=true")
        .isEqualTo(committed);
  }

  /**
   * Regression (fresh-eyes review, slice 5.1): springdoc keys component schemas by SIMPLE class
   * name, so two different records named the same collapse to one schema — silently overwriting it.
   * A guide item record named {@code Item} corrupted {@code ClinicalDocumentListResponse}'s item
   * schema (SPEC-0011) with guide fields ({@code tussCode}/{@code quantity}). The drift test above
   * cannot catch this (both the generated and committed snapshots are consistently wrong). Assert
   * the clinical-document list item keeps its own fields.
   */
  @Test
  void clinicalDocumentListItemSchema_isNotOverwrittenByASchemaNameCollision() throws Exception {
    String body =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode schemas =
        JsonMapper.builder().build().readTree(body).path("components").path("schemas");
    String ref =
        schemas
            .path("ClinicalDocumentListResponse")
            .path("properties")
            .path("items")
            .path("items")
            .path("$ref")
            .asText();
    JsonNode itemProps = schemas.path(ref.substring(ref.lastIndexOf('/') + 1)).path("properties");
    assertThat(itemProps.has("professional"))
        .as("ClinicalDocumentListResponse item schema lost its clinical fields")
        .isTrue();
    assertThat(itemProps.has("tussCode"))
        .as("ClinicalDocumentListResponse item schema was overwritten by a guide-item schema")
        .isFalse();
  }
}
