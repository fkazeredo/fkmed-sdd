package com.fkmed.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** SPEC-0003 BR10: the 12-month retention cutoff math and delegation (unit-level). */
@ExtendWith(MockitoExtension.class)
class AuditRetentionServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-04T00:00:00Z"), ZoneOffset.UTC);

  @Mock private AuditEventRepository events;

  @Test
  void retentionCutoff_isTwelveMonthsBeforeNow() {
    AuditRetentionService service = new AuditRetentionService(events, CLOCK);
    assertThat(service.retentionCutoff()).isEqualTo(Instant.parse("2025-07-04T00:00:00Z"));
  }

  @Test
  void purgeOlderThan_delegatesToTheRepository() {
    Instant cutoff = Instant.parse("2025-01-01T00:00:00Z");
    when(events.deleteOlderThan(cutoff)).thenReturn(7);

    AuditRetentionService service = new AuditRetentionService(events, CLOCK);

    assertThat(service.purgeOlderThan(cutoff)).isEqualTo(7);
    verify(events).deleteOlderThan(cutoff);
  }

  @Test
  void purgeExpired_usesTheTwelveMonthCutoff() {
    Instant cutoff = Instant.parse("2025-07-04T00:00:00Z");
    when(events.deleteOlderThan(cutoff)).thenReturn(3);

    AuditRetentionService service = new AuditRetentionService(events, CLOCK);

    assertThat(service.purgeExpired()).isEqualTo(3);
    verify(events).deleteOlderThan(cutoff);
  }
}
