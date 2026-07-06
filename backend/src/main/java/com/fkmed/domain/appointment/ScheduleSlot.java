package com.fkmed.domain.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;

/**
 * A bookable time slot with finite capacity (SPEC-0009 BR5/BR6). The {@code occupied} count is
 * protected by an optimistic {@link Version} lock: two confirmations racing for the last seat both
 * pass the in-memory guard, but only the first {@code UPDATE ... WHERE version = ?} wins; the
 * loser's version mismatch is translated fail-fast to {@link SlotUnavailableException} by the
 * application service (BR6/AC3, no retry loop — ADR-0012).
 */
@Entity
@Table(name = "schedule_slot")
@Getter
public class ScheduleSlot {

  @Id private UUID id;

  @Column(name = "agenda_id", nullable = false)
  private UUID agendaId;

  @Column(name = "slot_date", nullable = false)
  private LocalDate slotDate;

  @Column(name = "slot_time", nullable = false)
  private LocalTime slotTime;

  @Column(nullable = false)
  private int capacity;

  @Column(nullable = false)
  private int occupied;

  @Version
  @Column(nullable = false)
  private long version;

  /** JPA only. */
  protected ScheduleSlot() {}

  /** Free seats remaining (never negative). */
  public int remaining() {
    return Math.max(0, capacity - occupied);
  }

  /** Whether the slot still has a free seat. */
  public boolean hasCapacity() {
    return remaining() > 0;
  }

  /**
   * Takes one seat.
   *
   * @throws SlotUnavailableException when the slot is already full (the in-memory guard; the
   *     version lock covers the concurrent last-seat race at flush time).
   */
  void occupy() {
    if (!hasCapacity()) {
      throw new SlotUnavailableException();
    }
    this.occupied++;
  }

  /** Releases one seat on cancellation or reschedule (BR9/BR10); never drops below zero. */
  void release() {
    if (this.occupied > 0) {
      this.occupied--;
    }
  }
}
