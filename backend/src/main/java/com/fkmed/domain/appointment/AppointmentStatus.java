package com.fkmed.domain.appointment;

import java.util.Set;

/**
 * The appointment lifecycle state machine (SPEC-0009 BR11).
 *
 * <p>Kept as an enum under invariant 7 / DECISIONS-BASELINE §0019: it is a lifecycle state machine
 * whose transitions the code enforces — {@code AGENDADO -> REAGENDADO | CANCELADO | REALIZADO} and
 * {@code REAGENDADO -> CANCELADO | REALIZADO}, with {@code CANCELADO} and {@code REALIZADO} final —
 * not reference data. Only {@link #AGENDADO}, {@link #REAGENDADO} and {@link #CANCELADO} are ever
 * persisted; {@link #REALIZADO} is derived on read once the start instant passes without a
 * cancellation (BR12), so it is never stored.
 */
public enum AppointmentStatus {
  AGENDADO,
  REAGENDADO,
  CANCELADO,
  REALIZADO;

  private static final Set<AppointmentStatus> ACTIVE = Set.of(AGENDADO, REAGENDADO);

  /** Whether the appointment is still an open commitment that may be cancelled or rescheduled. */
  public boolean isActive() {
    return ACTIVE.contains(this);
  }

  /**
   * Whether a transition to {@code target} is allowed by the state machine (BR11). Used to protect
   * cancel/reschedule against an already-closed appointment.
   */
  public boolean canTransitionTo(AppointmentStatus target) {
    return switch (this) {
      case AGENDADO -> target == REAGENDADO || target == CANCELADO || target == REALIZADO;
      case REAGENDADO -> target == CANCELADO || target == REALIZADO;
      case CANCELADO, REALIZADO -> false;
    };
  }
}
