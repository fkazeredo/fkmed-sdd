package com.fkmed.domain.telemedicine;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Command/read access to {@code tele_session} (SPEC-0010). */
public interface TeleSessionRepository extends JpaRepository<TeleSession, UUID> {

  /** The caller's current live session across the beneficiaries they may act for (most recent). */
  Optional<TeleSession> findFirstByBeneficiaryIdInAndStateInOrderByCreatedAtDesc(
      Collection<UUID> beneficiaryIds, Collection<TeleSessionState> states);

  /** The beneficiary's live session of a given type, for the single-active-session resume (BR7). */
  Optional<TeleSession> findFirstByBeneficiaryIdAndTypeAndStateInOrderByCreatedAtDesc(
      UUID beneficiaryId, TeleSessionType type, Collection<TeleSessionState> states);

  /** The live scheduled session bridged from an appointment, for the join resume (BR14). */
  Optional<TeleSession> findFirstByAppointmentIdAndStateIn(
      UUID appointmentId, Collection<TeleSessionState> states);

  /**
   * The next walk-in session waiting in the queue (oldest first) — the operator's "attend next".
   */
  Optional<TeleSession> findFirstByTypeAndStateOrderByQueueEnteredAtAsc(
      TeleSessionType type, TeleSessionState state);

  /**
   * How many walk-in sessions are queued ahead of the given entry instant (queue position, BR5).
   */
  long countByTypeAndStateAndQueueEnteredAtBefore(
      TeleSessionType type, TeleSessionState state, Instant queueEnteredAt);

  /** Sessions attended but not yet responded to before the deadline — the no-show sweep (BR8). */
  List<TeleSession> findByStateAndStartedAtIsNullAndCalledAtBefore(
      TeleSessionState state, Instant deadline);
}
