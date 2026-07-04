package com.fkmed.domain.audit;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The append-only audit-recording contract (SPEC-0003 BR6) consumed by every module. Stamps the
 * entry with the current instant and persists it; there is intentionally no update or delete method
 * here (BR7 immutability). Runs in the caller's transaction (default propagation) so the record is
 * atomic with the action it audits (BR4), and starts its own when there is none (login/logout).
 */
@Service
@RequiredArgsConstructor
public class AuditRecorder {

  private final AuditEventRepository events;
  private final Clock clock;

  /** Appends one masked audit entry to the trail. */
  @Transactional
  public void record(AuditEntry entry) {
    events.save(AuditEvent.of(entry, clock.instant()));
  }
}
