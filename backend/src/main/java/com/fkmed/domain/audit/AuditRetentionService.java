package com.fkmed.domain.audit;

import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enforces the 12-month audit retention (SPEC-0003 BR10, owner decision Phase 1). Deletes only
 * entries strictly older than the cutoff — an entry exactly at the cutoff instant is retained
 * (AC6). Time comes from an injected {@link Clock} so the sweep is fully test-controllable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditRetentionService {

  /** Owner decision (SPEC-0003 BR10): audit entries live for twelve months. */
  static final Period RETENTION = Period.ofMonths(12);

  private final AuditEventRepository events;
  private final Clock clock;

  /** The instant before which entries are purged: now minus the 12-month retention. */
  public Instant retentionCutoff() {
    return ZonedDateTime.now(clock).minus(RETENTION).toInstant();
  }

  /** Purges entries older than an explicit cutoff (used by tests with a controlled instant). */
  @Transactional
  public int purgeOlderThan(Instant cutoff) {
    int deleted = events.deleteOlderThan(cutoff);
    if (deleted > 0) {
      log.info("audit retention: purged {} entries older than {}", deleted, cutoff);
    }
    return deleted;
  }

  /** Purges entries past the 12-month retention (invoked by the scheduled job). */
  @Transactional
  public int purgeExpired() {
    return purgeOlderThan(retentionCutoff());
  }
}
