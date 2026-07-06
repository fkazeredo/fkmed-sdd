package com.fkmed.domain.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
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

  /** One time slot with the seats still free. */
  public record Slot(LocalTime time, int remaining) {

    /** Whether the slot can still be chosen (has a free seat). */
    public boolean available() {
      return remaining > 0;
    }
  }
}
