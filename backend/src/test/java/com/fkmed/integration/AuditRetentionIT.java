package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.audit.AuditRetentionService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SPEC-0003 AC6/BR10: the 12-month retention sweep deletes only entries older than the cutoff, over
 * a real Postgres and with an explicit (clock-controlled) cutoff. Includes the boundary case: an
 * entry exactly at the cutoff instant is retained.
 */
class AuditRetentionIT extends AbstractIntegrationTest {

  private static final Instant CUTOFF = Instant.parse("2025-07-04T00:00:00Z");

  @Autowired private JdbcTemplate jdbc;
  @Autowired private AuditRetentionService retention;

  @BeforeEach
  void clearTrail() {
    jdbc.update("delete from audit_event");
  }

  @Test
  void purgeOlderThan_deletesOnlyEntriesBeforeTheCutoff() {
    insertAudit("2025-07-03T23:59:59Z"); // older than the cutoff → deleted
    insertAudit("2025-07-04T00:00:00Z"); // exactly at the cutoff → retained
    insertAudit("2025-07-05T00:00:00Z"); // newer → retained

    int deleted = retention.purgeOlderThan(CUTOFF);

    assertThat(deleted).isEqualTo(1);
    assertThat(count()).isEqualTo(2);
    assertThat(earliestRemaining()).isEqualTo("2025-07-04T00:00:00Z");
  }

  private void insertAudit(String occurredAtIso) {
    jdbc.update(
        "insert into audit_event (id, occurred_at, event_type, details) values"
            + " (gen_random_uuid(), ?::timestamptz, 'test.event', '{}'::jsonb)",
        occurredAtIso);
  }

  private long count() {
    return jdbc.queryForObject("select count(*) from audit_event", Long.class);
  }

  private String earliestRemaining() {
    return jdbc.queryForObject(
        "select to_char(min(occurred_at) at time zone 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')"
            + " from audit_event",
        String.class);
  }
}
