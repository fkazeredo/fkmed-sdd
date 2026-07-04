package com.fkmed.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base of the MockMvc integration suite: real Postgres (Testcontainers), Flyway migrations and the
 * dev profile (the SPEC-0002 login seam under test in this slice).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public abstract class AbstractIntegrationTest {

  @ServiceConnection static final PostgreSQLContainer POSTGRES = SharedPostgres.INSTANCE;
}
