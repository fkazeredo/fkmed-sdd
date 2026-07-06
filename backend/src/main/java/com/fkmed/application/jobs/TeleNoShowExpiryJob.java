package com.fkmed.application.jobs;

import com.fkmed.domain.telemedicine.TeleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sweeps the telemedicine queue for no-shows (SPEC-0010 BR8/AC3): a session whose turn was reached
 * but which has not responded within the 5-minute window is expired as {@code ABANDONADA}. Runs on
 * a short fixed delay; the 5-minute rule itself lives in {@code domain.telemedicine} against the
 * application {@code Clock}, so the sweep cadence only bounds how quickly the expiry is applied.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TeleNoShowExpiryJob {

  private final TeleService tele;

  @Scheduled(fixedDelayString = "${app.tele.no-show-sweep-ms:30000}")
  void sweep() {
    int expired = tele.expireNoShows();
    if (expired > 0) {
      log.info("tele no-show sweep expired {} session(s)", expired);
    }
  }
}
