package com.fkmed.domain.telemedicine;

import com.fkmed.domain.appointment.AppointmentService;
import com.fkmed.domain.appointment.TeleJoinTarget;
import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Application service and public facade of the telemedicine module (SPEC-0010). Owns the Pronto
 * Atendimento queue (enter/resume/leave), the live session read, the no-show sweep, the scheduled
 * teleconsultation join and the professional-side seams the operator simulation drives ({@link
 * #reachTurn}, {@link #close}).
 *
 * <p>Family scope is enforced through {@link BeneficiaryAccess} (BR13); the single active session
 * per beneficiary (BR7) is guaranteed by a partial unique index, with the concurrent double-POST
 * race translated to a resume on a fresh read (DL-0005 precedent). The state machine ({@link
 * TeleSessionState}) is guarded by the session's optimistic {@code @Version}. Transitions publish
 * their domain events inside the transaction; the notification listener and the
 * closure&rarr;documents issuance consume them AFTER_COMMIT in Wave 2.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeleService {

  /** No response within this window after the turn expires the session as ABANDONADA (BR8/AC3). */
  static final Duration NO_SHOW_TIMEOUT = Duration.ofMinutes(5);

  /** "Entrar na consulta" opens this long before the scheduled slot (BR14). */
  static final Duration JOIN_WINDOW_BEFORE = Duration.ofMinutes(10);

  /** The tele slot length; the join window closes at the slot's end (BR14, V19 30-minute slots). */
  static final Duration TELE_SLOT_DURATION = Duration.ofMinutes(30);

  /**
   * A queued session holds its position across up to this much disconnection before the ordinary
   * lifecycle resumes (DL-0017): reconnecting within it resumes the same session. A tuning constant
   * — queued sessions are not auto-expired on disconnect, they simply persist, so a reconnection at
   * any time finds the live session (BR6).
   */
  static final Duration DISCONNECT_HOLD = Duration.ofMinutes(2);

  private static final List<TeleSessionState> ACTIVE_STATES =
      List.of(TeleSessionState.EM_FILA, TeleSessionState.EM_ATENDIMENTO);

  private final SymptomRepository symptoms;
  private final TeleTermRepository teleTerms;
  private final TeleSessionRepository sessions;
  private final BeneficiaryAccess beneficiaryAccess;
  private final AppointmentService appointments;
  private final ApplicationEventPublisher events;
  private final MeterRegistry metrics;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  /** The triage catalog: the symptom registry plus the current teleattendance term (BR2/BR4). */
  @Transactional(readOnly = true)
  public TeleCatalogView catalog() {
    List<TeleCatalogView.SymptomOption> options =
        symptoms.findAllByOrderByNameAsc().stream()
            .map(s -> new TeleCatalogView.SymptomOption(s.getCode(), s.getName()))
            .toList();
    TeleTerm term = currentTerm();
    return new TeleCatalogView(
        options, new TeleCatalogView.TeleTermView(term.getVersion(), term.getBody()));
  }

  /**
   * Enters the Pronto Atendimento queue as {@code EM_FILA} (BR5), or resumes the beneficiary's
   * existing active session (BR7). Validates the family scope (BR13), the complaint length (BR2),
   * the accepted term version (BR4) and the triage symptoms/duration (BR2). The
   * single-active-session partial unique index makes the concurrent double-POST race safe: the
   * loser's insert conflict is translated to a resume on a fresh read (DL-0005 precedent).
   *
   * @throws com.fkmed.domain.plan.BeneficiaryNotAccessibleException when the beneficiary is out of
   *     scope; {@link TeleComplaintInvalidException}, {@link TeleTermNotAcceptedException} and
   *     {@link TeleTriageInvalidException} per the validation rules above.
   */
  public EnterQueueResult enterQueue(EnterQueueCommand command) {
    AccessibleBeneficiary beneficiary =
        beneficiaryAccess.requireAccessible(command.callerCard(), command.beneficiaryId());
    UUID beneficiaryId = beneficiary.beneficiaryId();
    validateComplaint(command.complaint());
    validateTerm(command.termVersion());
    validateTriage(command.symptoms(), command.duration());

    Optional<TeleSession> existing = activeWalkIn(beneficiaryId);
    if (existing.isPresent()) {
      metrics.counter("tele.session.resumed").increment();
      return resultOf(existing.get(), true);
    }
    try {
      TeleSession created =
          new TransactionTemplate(transactionManager)
              .execute(status -> openWalkIn(command, beneficiaryId));
      metrics.counter("tele.session.entered").increment();
      log.info(
          "tele session {} entered queue for beneficiary {}", created.getId(), mask(beneficiaryId));
      return resultOf(created, false);
    } catch (DataIntegrityViolationException race) {
      // Concurrent double-POST lost the single-active-session index — resume the winner (BR7).
      metrics.counter("tele.session.resume_race").increment();
      TeleSession winner =
          activeWalkIn(beneficiaryId).orElseThrow(TeleSessionNotFoundException::new);
      return resultOf(winner, true);
    }
  }

  /**
   * The caller's current live session (id + view) across the beneficiaries they may act for, or
   * empty (BR6). Observing the room while being attended records the beneficiary's response and
   * cancels the no-show timer (BR8). The id lets the SSE layer target re-emits at this session.
   */
  @Transactional
  public Optional<TeleCurrentSession> currentSessionFor(String callerCard) {
    Set<UUID> ids = accessibleIds(callerCard);
    if (ids.isEmpty()) {
      return Optional.empty();
    }
    Optional<TeleSession> current =
        sessions.findFirstByBeneficiaryIdInAndStateInOrderByCreatedAtDesc(ids, ACTIVE_STATES);
    current.ifPresent(
        session -> {
          if (session.markResponded(clock.instant())) {
            sessions.save(session);
          }
        });
    return current.map(session -> new TeleCurrentSession(session.getId(), toView(session)));
  }

  /**
   * Leaves the caller's current session as {@code ABANDONADA}, releasing its queue place (BR5).
   *
   * @throws TeleSessionNotFoundException when the caller has no active session.
   */
  @Transactional
  public TeleSessionView leave(String callerCard) {
    Set<UUID> ids = accessibleIds(callerCard);
    TeleSession session =
        (ids.isEmpty()
                ? Optional.<TeleSession>empty()
                : sessions.findFirstByBeneficiaryIdInAndStateInOrderByCreatedAtDesc(
                    ids, ACTIVE_STATES))
            .orElseThrow(TeleSessionNotFoundException::new);
    session.leave(clock.instant());
    sessions.save(session);
    metrics.counter("tele.session.left").increment();
    log.info("tele session {} left the queue", session.getId());
    return toView(session);
  }

  /**
   * Opens the room for a scheduled teleconsultation when the caller joins within the window — from
   * 10 minutes before the slot until its end (BR14, DL-0018) — creating (or resuming) a {@code
   * SCHEDULED} session. The appointment is scope-checked by the appointment module.
   *
   * @throws com.fkmed.domain.appointment.AppointmentNotFoundException when the appointment is
   *     unknown or out of scope; {@link TeleSessionNotFoundException} when it is not a joinable
   *     teleconsultation; {@link TeleJoinWindowClosedException} outside the window.
   */
  public TeleSessionView joinScheduled(String callerCard, UUID appointmentId) {
    TeleJoinTarget target = appointments.teleJoinTarget(callerCard, appointmentId);
    if (!target.telemedicine() || !target.active()) {
      throw new TeleSessionNotFoundException();
    }
    if (!withinJoinWindow(target.scheduledAt(), clock.instant())) {
      throw new TeleJoinWindowClosedException();
    }
    TeleSession session =
        sessions
            .findFirstByAppointmentIdAndStateIn(appointmentId, ACTIVE_STATES)
            .orElseGet(() -> openScheduled(target));
    return toView(session);
  }

  /**
   * Marks that it is the beneficiary's turn: the professional starts attending (BR8/BR9) and {@link
   * TeleTurnReached} is published. The seam the operator simulation (SPEC-0018) drives.
   *
   * @throws TeleSessionNotFoundException when the session is unknown.
   * @throws IllegalStateException when the session is not waiting in the queue.
   */
  @Transactional
  public void reachTurn(UUID sessionId, String professionalName, String professionalCrm) {
    TeleSession session =
        sessions.findById(sessionId).orElseThrow(TeleSessionNotFoundException::new);
    session.reachTurn(professionalName, professionalCrm, clock.instant());
    sessions.save(session);
    metrics.counter("tele.turn.reached").increment();
    log.info(
        "tele session {} reached the turn for beneficiary {}",
        sessionId,
        mask(session.getBeneficiaryId()));
    events.publishEvent(
        new TeleTurnReached(
            session.getId(),
            session.getBeneficiaryId(),
            session.getProfessionalName(),
            session.getCreatedBy()));
  }

  /**
   * Closes the session as {@code ENCERRADA} with the professional's summary (BR9) and publishes
   * {@link TeleSessionClosed}. The seam the operator simulation (SPEC-0018) drives; Wave 2 wires
   * the clinical-document issuance off the event.
   *
   * @throws TeleSessionNotFoundException when the session is unknown.
   * @throws IllegalStateException when the session is not being attended.
   */
  @Transactional
  public void close(UUID sessionId, TeleClosureSummary summary) {
    TeleSession session =
        sessions.findById(sessionId).orElseThrow(TeleSessionNotFoundException::new);
    session.close(
        summary.professionalName(), summary.professionalCrm(), summary.guidance(), clock.instant());
    sessions.save(session);
    metrics.counter("tele.session.closed").increment();
    log.info(
        "tele session {} closed for beneficiary {}", sessionId, mask(session.getBeneficiaryId()));
    events.publishEvent(
        new TeleSessionClosed(
            session.getId(),
            session.getBeneficiaryId(),
            session.getProfessionalName(),
            session.getProfessionalCrm(),
            session.getGuidance(),
            session.getCreatedBy(),
            session.getEndedAt()));
  }

  /**
   * Expires the sessions whose turn was reached but which have not responded within the 5-minute
   * no-show window, as {@code ABANDONADA} (BR8/AC3). Driven periodically by the delivery-layer job.
   *
   * @return the number of sessions expired in this sweep.
   */
  @Transactional
  public int expireNoShows() {
    Instant now = clock.instant();
    Instant deadline = now.minus(NO_SHOW_TIMEOUT);
    List<TeleSession> stale =
        sessions.findByStateAndStartedAtIsNullAndCalledAtBefore(
            TeleSessionState.EM_ATENDIMENTO, deadline);
    for (TeleSession session : stale) {
      session.expireAsNoShow(now);
      sessions.save(session);
      metrics.counter("tele.session.no_show").increment();
      log.info("tele session {} expired as no-show", session.getId());
    }
    return stale.size();
  }

  /** The recomputed live view of a session by id, for the SSE re-emit; empty once final. */
  @Transactional(readOnly = true)
  public Optional<TeleSessionView> viewOf(UUID sessionId) {
    return sessions.findById(sessionId).filter(TeleSession::isActive).map(this::toView);
  }

  // --- internals ---

  private TeleSession openWalkIn(EnterQueueCommand command, UUID beneficiaryId) {
    TeleSession session =
        TeleSession.walkIn(
            beneficiaryId,
            command.complaint(),
            command.symptoms(),
            command.otherSymptom(),
            command.duration(),
            command.termVersion(),
            command.authorAccountId(),
            clock.instant());
    return sessions.saveAndFlush(session);
  }

  private TeleSession openScheduled(TeleJoinTarget target) {
    try {
      return new TransactionTemplate(transactionManager)
          .execute(
              status ->
                  sessions.saveAndFlush(
                      TeleSession.scheduled(
                          target.beneficiaryId(),
                          target.appointmentId(),
                          target.authorAccountId(),
                          clock.instant())));
    } catch (DataIntegrityViolationException race) {
      // A concurrent join for the same appointment already opened the room — resume it (BR14).
      return sessions
          .findFirstByAppointmentIdAndStateIn(target.appointmentId(), ACTIVE_STATES)
          .orElseThrow(TeleSessionNotFoundException::new);
    }
  }

  private Optional<TeleSession> activeWalkIn(UUID beneficiaryId) {
    return sessions.findFirstByBeneficiaryIdAndTypeAndStateInOrderByCreatedAtDesc(
        beneficiaryId, TeleSessionType.WALK_IN, ACTIVE_STATES);
  }

  private Set<UUID> accessibleIds(String callerCard) {
    return beneficiaryAccess.accessibleFor(callerCard).stream()
        .map(AccessibleBeneficiary::beneficiaryId)
        .collect(Collectors.toSet());
  }

  private boolean withinJoinWindow(Instant scheduledAt, Instant now) {
    Instant opens = scheduledAt.minus(JOIN_WINDOW_BEFORE);
    Instant closes = scheduledAt.plus(TELE_SLOT_DURATION);
    return !now.isBefore(opens) && !now.isAfter(closes);
  }

  private void validateComplaint(String complaint) {
    int length = complaint == null ? 0 : complaint.strip().length();
    if (length < 10 || length > 500) {
      throw new TeleComplaintInvalidException();
    }
  }

  private void validateTerm(String termVersion) {
    if (termVersion == null || !termVersion.equals(currentTerm().getVersion())) {
      throw new TeleTermNotAcceptedException();
    }
  }

  private void validateTriage(List<String> symptomCodes, String durationCode) {
    if (!SymptomDurationCodes.isValid(durationCode)) {
      throw new TeleTriageInvalidException();
    }
    if (symptomCodes != null) {
      for (String code : symptomCodes) {
        if (code == null || !symptoms.existsById(code)) {
          throw new TeleTriageInvalidException();
        }
      }
    }
  }

  private TeleTerm currentTerm() {
    return teleTerms
        .findTopByOrderByPublishedAtDesc()
        .orElseThrow(() -> new IllegalStateException("no teleattendance term is published"));
  }

  private EnterQueueResult resultOf(TeleSession session, boolean resumed) {
    int position = queuePosition(session);
    return new EnterQueueResult(
        session.getState().name(), position, TeleQueue.etaMinutes(position), resumed);
  }

  private TeleSessionView toView(TeleSession session) {
    Integer position = null;
    Integer eta = null;
    if (session.getType() == TeleSessionType.WALK_IN
        && session.getState() == TeleSessionState.EM_FILA) {
      int computed = queuePosition(session);
      position = computed;
      eta = TeleQueue.etaMinutes(computed);
    }
    TeleSessionView.Professional professional =
        session.getProfessionalName() == null
            ? null
            : new TeleSessionView.Professional(
                session.getProfessionalName(), session.getProfessionalCrm());
    String room =
        session.getState() == TeleSessionState.EM_ATENDIMENTO ? "tele-" + session.getId() : null;
    return new TeleSessionView(session.getState().name(), position, eta, professional, room);
  }

  private int queuePosition(TeleSession session) {
    if (session.getType() != TeleSessionType.WALK_IN
        || session.getState() != TeleSessionState.EM_FILA) {
      return 0;
    }
    long ahead =
        sessions.countByTypeAndStateAndQueueEnteredAtBefore(
            TeleSessionType.WALK_IN, TeleSessionState.EM_FILA, session.getQueueEnteredAt());
    return TeleQueue.positionFrom(ahead);
  }

  /** Masks a beneficiary id in business logs (LGPD hygiene — security.md §Privacy). */
  private static String mask(UUID beneficiaryId) {
    String id = beneficiaryId.toString();
    return "***" + id.substring(id.length() - 4);
  }
}
