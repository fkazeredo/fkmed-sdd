package com.fkmed.infra.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata; the committed snapshot in {@code docs/api/openapi.json} is the contract. */
@Configuration
public class OpenApiConfig {

  /**
   * Kept in lockstep with {@code backend/pom.xml <version>} (DECISIONS-BASELINE §0015); the {@code
   * /release} skill bumps both together.
   */
  public static final String VERSION = "0.2.0";

  @Bean
  OpenAPI fkmedOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("FKMed")
                .description("FKMed — health plan beneficiary portal API")
                .version(VERSION));
  }
}
