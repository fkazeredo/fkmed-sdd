package com.fkmed.integration;

import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Singleton Postgres 16 container shared by the whole integration suite (one database per JVM; Ryuk
 * reclaims it after the run).
 */
public final class SharedPostgres {

  public static final PostgreSQLContainer INSTANCE = new PostgreSQLContainer("postgres:16-alpine");

  static {
    INSTANCE.start();
  }

  private SharedPostgres() {}
}
