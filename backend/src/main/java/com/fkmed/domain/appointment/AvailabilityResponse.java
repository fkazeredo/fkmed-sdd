package com.fkmed.domain.appointment;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The availability calendar for a (unit, scope) pair (SPEC-0009 §availability endpoint): the days
 * within the today..+30 horizon that have slots respecting the 2-hour antecedence (DL-0013), each
 * with its time slots and remaining capacity. A slot with {@code remaining == 0} is rendered
 * unavailable by the client (BR5: full slots must render unavailable).
 */
public record AvailabilityResponse(
    UUID unitId, AppointmentType scopeType, String scopeCode, List<Day> days) {

  /** One calendar day with its bookable time slots. */
  public record Day(LocalDate date, List<Slot> slots) {}

  /**
   * One time slot: {@code slot} is the ISO local datetime the client sends back to book (e.g.
   * {@code 2026-07-08T09:00}, matching the {@code POST /appointments} {@code slot} field), {@code
   * remaining} the seats still free and {@code available} whether it can still be chosen — a full
   * slot comes back with {@code remaining == 0}/{@code available == false} so the client renders it
   * unselectable rather than hiding it (BR5).
   */
  public record Slot(String slot, int remaining, boolean available) {}
}
