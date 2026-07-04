package com.fkmed.infra.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/** SPEC-0001 BR1: the probe answers false (never throws) when the database is unreachable. */
class DatabaseStatusProbeTest {

  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final DatabaseStatusProbe probe = new DatabaseStatusProbe(jdbc);

  @Test
  void reachableDatabase_isUp() {
    when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
    assertThat(probe.isUp()).isTrue();
  }

  @Test
  void unreachableDatabase_isDown_withoutPropagatingTheException() {
    when(jdbc.queryForObject(anyString(), eq(Integer.class)))
        .thenThrow(new DataAccessResourceFailureException("connection refused"));
    assertThat(probe.isUp()).isFalse();
  }
}
