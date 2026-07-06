package com.fkmed.domain.plan;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared protocol generator (SPEC-0003 BR9, DL-0016). It lives in the plan module — the module that
 * owns SPEC-0003 — rather than in a per-feature module, because BR9 is not appointment-specific:
 * SPEC-0009 is only its first consumer (prefix {@code AG-}), with future {@code RE-}/{@code PV-}
 * reusing the same service.
 *
 * <p>Produces a unique protocol matching {@code ^[A-Z]{2}-\d{8}-\d{4}$} (e.g. {@code
 * AG-20260705-0001}) from a per-prefix, per-day counter ({@code protocol_sequence}) incremented
 * atomically via a single {@code INSERT ... ON CONFLICT ... RETURNING}. The row lock the upsert
 * takes serialises concurrent callers, so two bookings never receive the same protocol (the same
 * concurrency concern as slot capacity — DL-0005 precedent). The counter resets each calendar day
 * in the product timezone because the key includes the date.
 */
@Service
@RequiredArgsConstructor
public class ProtocolGenerator {

  private static final Pattern PREFIX = Pattern.compile("^[A-Z]{2}$");
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final JdbcTemplate jdbc;
  private final Clock clock;

  /**
   * The next unique protocol for the given two-letter prefix, of the form {@code PREFIX-YYYYMMDD-
   * NNNN}. Joins the caller's transaction so a rolled-back booking does not consume a number.
   *
   * @throws IllegalArgumentException when the prefix is not exactly two uppercase letters.
   */
  @Transactional
  public String next(String prefix) {
    if (prefix == null || !PREFIX.matcher(prefix).matches()) {
      throw new IllegalArgumentException(
          "protocol prefix must be exactly two uppercase letters: " + prefix);
    }
    LocalDate date = LocalDate.now(clock);
    Integer counter =
        jdbc.queryForObject(
            "insert into protocol_sequence (prefix, seq_date, counter) values (?, ?, 1)"
                + " on conflict (prefix, seq_date)"
                + " do update set counter = protocol_sequence.counter + 1"
                + " returning counter",
            Integer.class,
            prefix,
            date);
    return format(prefix, date, counter == null ? 1 : counter);
  }

  /**
   * Formats a protocol from its parts (extracted so the {@code ^[A-Z]{2}-\d{8}-\d{4}$} shape is
   * unit-testable without a database).
   */
  static String format(String prefix, LocalDate date, int counter) {
    return "%s-%s-%04d".formatted(prefix, DATE.format(date), counter);
  }
}
