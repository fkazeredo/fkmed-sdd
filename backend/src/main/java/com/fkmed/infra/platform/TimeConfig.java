package com.fkmed.infra.platform;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The application {@link Clock} (product timezone {@code America/Sao_Paulo} — docs/specs/README.md
 * §UI norms). Domain services inject it so instants and calendar-date rules (e.g. the BR3 age
 * check, BR10 retention cutoff) are deterministic and test-controllable — never the JVM default
 * zone.
 */
@Configuration
public class TimeConfig {

  static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

  @Bean
  Clock clock() {
    return Clock.system(ZONE);
  }
}
