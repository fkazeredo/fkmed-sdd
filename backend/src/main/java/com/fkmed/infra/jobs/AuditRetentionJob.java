package com.fkmed.infra.jobs;

import com.fkmed.domain.audit.AuditRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled 12-month audit retention sweep (SPEC-0003 BR10). Delegates the cutoff and delete to
 * {@link AuditRetentionService} (which the tests drive directly with a controlled clock). Runs
 * daily off-peak; single-instance by default (baseline §0002).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditRetentionJob {

  private final AuditRetentionService retention;

  @Scheduled(cron = "${app.audit.retention-cron:0 30 3 * * *}", zone = "America/Sao_Paulo")
  void purgeExpiredAuditEntries() {
    int deleted = retention.purgeExpired();
    log.debug("audit retention sweep completed ({} entries removed)", deleted);
  }
}
