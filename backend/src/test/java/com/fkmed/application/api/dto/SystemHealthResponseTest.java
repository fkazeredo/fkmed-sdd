package com.fkmed.application.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Regression for review finding M1: the application status must reflect the application (which
 * answered the request), not mirror the database probe — only {@code database} does that.
 */
class SystemHealthResponseTest {

  @Test
  void databaseUp_reportsEverythingUp() {
    SystemHealthResponse response = SystemHealthResponse.of(true);
    assertThat(response.status()).isEqualTo("UP");
    assertThat(response.database()).isEqualTo("UP");
  }

  @Test
  void databaseDown_keepsApplicationUp_andReportsDatabaseDown() {
    SystemHealthResponse response = SystemHealthResponse.of(false);
    assertThat(response.status()).isEqualTo("UP");
    assertThat(response.database()).isEqualTo("DOWN");
  }
}
