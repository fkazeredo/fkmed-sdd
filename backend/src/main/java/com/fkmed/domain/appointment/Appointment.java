package com.fkmed.domain.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * A beneficiary's booking of a consultation or exam in an operator care unit (SPEC-0009). Carries
 * the unique protocol (BR7), the {@link AppointmentStatus} state machine (BR11) and the seat it
 * occupies. Only {@code AGENDADO}/{@code REAGENDADO}/{@code CANCELADO} are persisted; {@code
 * REALIZADO} is derived on read (BR12, {@link #effectiveStatus}).
 *
 * <p>Invariants: a consultation carries a specialty and no exam code, an exam carries an exam code
 * and no specialty (enforced here and by a DB check); cancel and reschedule are only allowed while
 * the appointment is an upcoming active commitment.
 */
@Entity
@Table(name = "appointment")
@Getter
public class Appointment {

  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private String protocol;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AppointmentType type;

  @Column(name = "beneficiary_id", nullable = false)
  private UUID beneficiaryId;

  @Column(name = "specialty_code")
  private String specialtyCode;

  @Column(name = "exam_code")
  private String examCode;

  @Column(name = "unit_id", nullable = false)
  private UUID unitId;

  @Column(name = "slot_id", nullable = false)
  private UUID slotId;

  @Column(name = "scheduled_at", nullable = false)
  private Instant scheduledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AppointmentStatus status;

  @Column(name = "cancel_reason")
  private String cancelReason;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** JPA only. */
  protected Appointment() {}

  private Appointment(
      AppointmentType type,
      UUID beneficiaryId,
      String specialtyCode,
      String examCode,
      UUID unitId,
      UUID slotId,
      Instant scheduledAt,
      String protocol,
      UUID createdBy,
      Instant now) {
    this.id = UUID.randomUUID();
    this.type = type;
    this.beneficiaryId = beneficiaryId;
    this.specialtyCode = specialtyCode;
    this.examCode = examCode;
    this.unitId = unitId;
    this.slotId = slotId;
    this.scheduledAt = scheduledAt;
    this.protocol = protocol;
    this.status = AppointmentStatus.AGENDADO;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Books a consultation as {@code AGENDADO} against a resolved specialty and seat (BR3/BR7).
   *
   * @throws IllegalArgumentException when the specialty code is missing.
   */
  static Appointment consultation(
      UUID beneficiaryId,
      String specialtyCode,
      UUID unitId,
      UUID slotId,
      Instant scheduledAt,
      String protocol,
      UUID createdBy,
      Instant now) {
    if (specialtyCode == null || specialtyCode.isBlank()) {
      throw new IllegalArgumentException("a consultation requires a specialty");
    }
    return new Appointment(
        AppointmentType.CONSULTATION,
        beneficiaryId,
        specialtyCode,
        null,
        unitId,
        slotId,
        scheduledAt,
        protocol,
        createdBy,
        now);
  }

  /**
   * Books an exam as {@code AGENDADO} against a resolved exam and seat (BR4/BR7).
   *
   * @throws IllegalArgumentException when the exam code is missing.
   */
  static Appointment exam(
      UUID beneficiaryId,
      String examCode,
      UUID unitId,
      UUID slotId,
      Instant scheduledAt,
      String protocol,
      UUID createdBy,
      Instant now) {
    if (examCode == null || examCode.isBlank()) {
      throw new IllegalArgumentException("an exam requires an exam code");
    }
    return new Appointment(
        AppointmentType.EXAM,
        beneficiaryId,
        null,
        examCode,
        unitId,
        slotId,
        scheduledAt,
        protocol,
        createdBy,
        now);
  }

  /** The specialty (consultation) or exam (exam) code this appointment books, for events/views. */
  public String scopeCode() {
    return type == AppointmentType.CONSULTATION ? specialtyCode : examCode;
  }

  /**
   * The status as seen on read: {@code REALIZADO} once an active appointment's start instant has
   * passed without cancellation (BR12), otherwise the persisted status.
   */
  public AppointmentStatus effectiveStatus(Instant now) {
    if (status.isActive() && !scheduledAt.isAfter(now)) {
      return AppointmentStatus.REALIZADO;
    }
    return status;
  }

  /** Whether the appointment is still an upcoming active commitment that may be changed (BR9). */
  public boolean isChangeable(Instant now) {
    return status.isActive() && scheduledAt.isAfter(now);
  }

  /**
   * Cancels the appointment, keeping it in history (BR9).
   *
   * @throws AppointmentTooLateException when the start time has passed or it is already closed.
   */
  void cancel(String reason, Instant now) {
    if (!isChangeable(now)) {
      throw new AppointmentTooLateException();
    }
    this.status = AppointmentStatus.CANCELADO;
    this.cancelReason = (reason == null || reason.isBlank()) ? null : reason;
    this.updatedAt = now;
  }

  /**
   * Reschedules to a new seat/instant, keeping beneficiary, specialty/exam, unit and protocol and
   * only moving the date/time (BR10).
   *
   * @throws AppointmentTooLateException when the start time has passed or it is already closed.
   */
  void rescheduleTo(UUID newSlotId, Instant newScheduledAt, Instant now) {
    if (!isChangeable(now)) {
      throw new AppointmentTooLateException();
    }
    this.slotId = newSlotId;
    this.scheduledAt = newScheduledAt;
    this.status = AppointmentStatus.REAGENDADO;
    this.updatedAt = now;
  }
}
