package com.fkmed.application.jobs;

import com.fkmed.domain.reimbursement.ReimbursementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cancels reimbursement pendencies unanswered for 30 days (SPEC-0016 BR7). */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReimbursementPendencyExpiryJob {

  private final ReimbursementService reimbursements;

  @Scheduled(cron = "0 20 2 * * *", zone = "America/Sao_Paulo")
  void cancelExpiredPendencies() {
    int cancelled = reimbursements.cancelExpiredPendencies();
    if (cancelled > 0) {
      log.info("cancelled {} expired reimbursement pendenc(y/ies)", cancelled);
    }
  }
}
