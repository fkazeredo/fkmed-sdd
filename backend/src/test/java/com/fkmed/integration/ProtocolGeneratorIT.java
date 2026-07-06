package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.plan.ProtocolGenerator;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * SPEC-0003 BR9 / DL-0016: the shared protocol generator against real Postgres — the per-prefix,
 * per-day atomic counter, its daily rollover (deterministic {@link MutableClock}) and uniqueness
 * under concurrency (the same guarantee slot capacity needs).
 */
@SpringBootTest
@ActiveProfiles("dev")
@Import(ProtocolGeneratorIT.FixedClockConfig.class)
class ProtocolGeneratorIT {

  @ServiceConnection static final PostgreSQLContainer POSTGRES = SharedPostgres.INSTANCE;

  static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  // 10:00 in São Paulo on 2026-07-06.
  static final Instant START = Instant.parse("2026-07-06T13:00:00Z");

  @Autowired private ProtocolGenerator generator;
  @Autowired private MutableClock clock;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    clock.reset(START);
    jdbc.update("delete from protocol_sequence");
  }

  @Test
  void increments_perDay_thenResetsOnTheNextDay() {
    assertThat(generator.next("AG")).isEqualTo("AG-20260706-0001");
    assertThat(generator.next("AG")).isEqualTo("AG-20260706-0002");

    clock.advance(Duration.ofDays(1));
    assertThat(generator.next("AG")).isEqualTo("AG-20260707-0001");
  }

  @Test
  void isolatesCountersPerPrefix() {
    assertThat(generator.next("AG")).isEqualTo("AG-20260706-0001");
    assertThat(generator.next("RE")).isEqualTo("RE-20260706-0001");
    assertThat(generator.next("AG")).isEqualTo("AG-20260706-0002");
  }

  @Test
  void neverProducesADuplicateUnderConcurrency() throws Exception {
    int callers = 24;
    CyclicBarrier gate = new CyclicBarrier(callers);
    ExecutorService pool = Executors.newFixedThreadPool(callers);
    Set<String> protocols = ConcurrentHashMap.newKeySet();
    try {
      List<Future<String>> futures =
          IntStream.range(0, callers)
              .mapToObj(
                  i ->
                      pool.submit(
                          () -> {
                            gate.await();
                            return generator.next("AG");
                          }))
              .toList();
      for (Future<String> future : futures) {
        protocols.add(future.get());
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(protocols).as("every concurrent caller gets a unique protocol").hasSize(callers);
    assertThat(protocols).allMatch(p -> p.matches("^AG-20260706-\\d{4}$"));
    assertThat(
            jdbc.queryForObject(
                "select counter from protocol_sequence where prefix = 'AG'", Integer.class))
        .isEqualTo(callers);
  }

  /** Overrides the application {@code Clock} with a test-advanceable one (BR9 daily rollover). */
  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(START, ZONE);
    }
  }
}
