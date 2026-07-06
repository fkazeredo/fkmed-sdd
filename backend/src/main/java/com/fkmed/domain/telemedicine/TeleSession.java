package com.fkmed.domain.telemedicine;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

/**
 * A telemedicine session (SPEC-0010): a beneficiary's place in the Pronto Atendimento queue or a
 * joined scheduled teleconsultation, carrying the {@link TeleSessionState} state machine (BR11).
 * The state is guarded by an optimistic {@link Version} lock so concurrent transitions and the
 * single-active-session race (BR7) let exactly one writer win.
 *
 * <p>Invariants: transitions go only through the state machine ({@link
 * TeleSessionState#canTransitionTo}); a {@code WALK_IN} session carries the triage (complaint,
 * symptoms, duration) and a queue position, a {@code SCHEDULED} session links to the appointment it
 * bridges from. The turn is reached once ({@link #reachTurn}); a no-show is only possible after the
 * turn and before the beneficiary responds ({@link #isNoShow}).
 */
@Entity
@Table(name = "tele_session")
@Getter
public class TeleSession {

  @Id private UUID id;

  @Column(name = "beneficiary_id", nullable = false)
  private UUID beneficiaryId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TeleSessionType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TeleSessionState state;

  private String complaint;

  @ElementCollection
  @CollectionTable(name = "tele_session_symptom", joinColumns = @JoinColumn(name = "session_id"))
  @Column(name = "symptom_code")
  private Set<String> symptomCodes = new HashSet<>();

  @Column(name = "other_symptom")
  private String otherSymptom;

  @Column(name = "duration_code")
  private String durationCode;

  @Column(name = "professional_name")
  private String professionalName;

  @Column(name = "professional_crm")
  private String professionalCrm;

  private String guidance;

  @Column(name = "term_version")
  private String termVersion;

  @Column(name = "appointment_id")
  private UUID appointmentId;

  @Column(name = "queue_entered_at")
  private Instant queueEnteredAt;

  @Column(name = "called_at")
  private Instant calledAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** JPA only. */
  protected TeleSession() {}

  private TeleSession(
      TeleSessionType type,
      UUID beneficiaryId,
      UUID appointmentId,
      String complaint,
      Collection<String> symptomCodes,
      String otherSymptom,
      String durationCode,
      String termVersion,
      UUID createdBy,
      Instant now) {
    this.id = UUID.randomUUID();
    this.type = type;
    this.beneficiaryId = beneficiaryId;
    this.appointmentId = appointmentId;
    this.complaint = complaint;
    this.symptomCodes = symptomCodes == null ? new HashSet<>() : new HashSet<>(symptomCodes);
    this.otherSymptom = blankToNull(otherSymptom);
    this.durationCode = durationCode;
    this.termVersion = termVersion;
    this.state = TeleSessionState.EM_FILA;
    this.queueEnteredAt = now;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Opens a Pronto Atendimento session as {@code EM_FILA} with the accepted triage (BR2/BR4/BR5).
   * The caller has already validated the complaint length, term version, symptoms and duration.
   */
  static TeleSession walkIn(
      UUID beneficiaryId,
      String complaint,
      Collection<String> symptomCodes,
      String otherSymptom,
      String durationCode,
      String termVersion,
      UUID createdBy,
      Instant now) {
    return new TeleSession(
        TeleSessionType.WALK_IN,
        beneficiaryId,
        null,
        complaint,
        symptomCodes,
        otherSymptom,
        durationCode,
        termVersion,
        createdBy,
        now);
  }

  /**
   * Opens a scheduled teleconsultation session as {@code EM_FILA} when the beneficiary joins their
   * appointment's room within the join window (BR14, DL-0018). Carries no triage; links to the
   * appointment it bridges from.
   */
  static TeleSession scheduled(
      UUID beneficiaryId, UUID appointmentId, UUID createdBy, Instant now) {
    return new TeleSession(
        TeleSessionType.SCHEDULED,
        beneficiaryId,
        appointmentId,
        null,
        null,
        null,
        null,
        null,
        createdBy,
        now);
  }

  /** Whether this session is still live (queued or being attended). */
  public boolean isActive() {
    return state.isActive();
  }

  /**
   * Marks that it is the beneficiary's turn: the professional starts attending, moving the session
   * to {@code EM_ATENDIMENTO} and recording the professional and the call instant (BR8/BR9). Driven
   * by the operator side (SPEC-0018 simulation in Wave 2).
   *
   * @throws IllegalStateException when the session is not waiting in the queue.
   */
  void reachTurn(String professionalName, String professionalCrm, Instant now) {
    transitionTo(TeleSessionState.EM_ATENDIMENTO, now);
    this.professionalName = professionalName;
    this.professionalCrm = professionalCrm;
    this.calledAt = now;
  }

  /**
   * Records that the beneficiary responded to their turn by observing the room, which starts the
   * participation and cancels the no-show timer (BR8). Idempotent — only the first observation sets
   * the start instant.
   *
   * @return {@code true} when this call recorded the response (state changed and must be
   *     persisted).
   */
  boolean markResponded(Instant now) {
    if (state == TeleSessionState.EM_ATENDIMENTO && startedAt == null) {
      this.startedAt = now;
      this.updatedAt = now;
      return true;
    }
    return false;
  }

  /**
   * Whether the session is a no-show: the turn was reached but the beneficiary has not responded
   * within {@code timeout} of the call (BR8/AC3).
   */
  boolean isNoShow(Instant now, Duration timeout) {
    return state == TeleSessionState.EM_ATENDIMENTO
        && startedAt == null
        && calledAt != null
        && !Duration.between(calledAt, now).minus(timeout).isNegative();
  }

  /**
   * Leaves the session as {@code ABANDONADA}, releasing the queue place (BR5 "Sair da fila").
   *
   * @throws IllegalStateException when the session is already final.
   */
  void leave(Instant now) {
    transitionTo(TeleSessionState.ABANDONADA, now);
    this.endedAt = now;
  }

  /**
   * Expires the session as {@code ABANDONADA} after the 5-minute no-show window (BR8/AC3).
   *
   * @throws IllegalStateException when the session is not being attended.
   */
  void expireAsNoShow(Instant now) {
    transitionTo(TeleSessionState.ABANDONADA, now);
    this.endedAt = now;
  }

  /**
   * Closes the session as {@code ENCERRADA} with the professional's summary (BR9). This is the seam
   * the operator simulation calls; Wave 2 wires the clinical-document issuance off the resulting
   * {@link TeleSessionClosed} event.
   *
   * @throws IllegalStateException when the session is not being attended.
   */
  void close(String professionalName, String professionalCrm, String guidance, Instant now) {
    transitionTo(TeleSessionState.ENCERRADA, now);
    if (professionalName != null) {
      this.professionalName = professionalName;
    }
    if (professionalCrm != null) {
      this.professionalCrm = professionalCrm;
    }
    this.guidance = blankToNull(guidance);
    this.endedAt = now;
  }

  private void transitionTo(TeleSessionState target, Instant now) {
    if (!state.canTransitionTo(target)) {
      throw new IllegalStateException("invalid tele-session transition " + state + " -> " + target);
    }
    this.state = target;
    this.updatedAt = now;
  }

  private static String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}
