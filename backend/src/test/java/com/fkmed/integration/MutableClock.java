package com.fkmed.integration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A {@link Clock} whose instant the test advances deterministically, so slice-1.2 timing rules (BR8
 * 15-minute lock window, BR10 30-minute reset-token TTL) are exercised over the real embedded
 * Authorization Server without waiting real wall-clock time. Overrides the application {@code
 * Clock} bean via a {@code @Primary @TestConfiguration} in the security ITs. Thread-safe: the
 * server threads read {@code instant()} while the test thread advances it.
 */
public final class MutableClock extends Clock {

  private final ZoneId zone;
  private volatile Instant now;

  public MutableClock(Instant start, ZoneId zone) {
    this.now = start;
    this.zone = zone;
  }

  @Override
  public Instant instant() {
    return now;
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId newZone) {
    return new MutableClock(now, newZone);
  }

  /** Moves the clock forward by {@code amount} (e.g. past the 15-minute lock window). */
  public void advance(Duration amount) {
    this.now = this.now.plus(amount);
  }

  /** Resets the clock to a known instant so each test starts from the same point. */
  public void reset(Instant instant) {
    this.now = instant;
  }
}
