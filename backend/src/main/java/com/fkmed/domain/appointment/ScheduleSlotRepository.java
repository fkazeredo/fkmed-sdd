package com.fkmed.domain.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Slot lookups for availability and booking. */
interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, UUID> {

  /** The slots of an agenda within a date window, ordered for availability rendering. */
  List<ScheduleSlot> findByAgendaIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(
      UUID agendaId, LocalDate from, LocalDate to);

  /** The exact slot for an agenda at a date+time, if the agenda offers it. */
  Optional<ScheduleSlot> findByAgendaIdAndSlotDateAndSlotTime(
      UUID agendaId, LocalDate slotDate, LocalTime slotTime);
}
