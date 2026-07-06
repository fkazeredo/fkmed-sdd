package com.fkmed.domain.appointment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Appointment persistence and the queries behind Meus Agendamentos + the BR8 conflict guard. */
interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

  /** Every appointment of the accessible beneficiaries, newest slot first (Meus Agendamentos). */
  List<Appointment> findByBeneficiaryIdInOrderByScheduledAtDesc(Collection<UUID> beneficiaryIds);

  /** One beneficiary's appointments, newest slot first (the Meus Agendamentos filter). */
  List<Appointment> findByBeneficiaryIdOrderByScheduledAtDesc(UUID beneficiaryId);

  /** An active ({@code AGENDADO}/{@code REAGENDADO}) appointment at the same instant (BR8). */
  Optional<Appointment> findFirstByBeneficiaryIdAndScheduledAtAndStatusIn(
      UUID beneficiaryId, Instant scheduledAt, Collection<AppointmentStatus> statuses);
}
