package com.fkmed.infra.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Checks database reachability for the public health endpoint (SPEC-0001 BR1). */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseStatusProbe {

  private final JdbcTemplate jdbcTemplate;

  /** {@code true} when the database answers a probe query. */
  public boolean isUp() {
    try {
      jdbcTemplate.queryForObject("select 1", Integer.class);
      return true;
    } catch (DataAccessException e) {
      log.warn("database health probe failed: {}", e.getMessage());
      return false;
    }
  }
}
