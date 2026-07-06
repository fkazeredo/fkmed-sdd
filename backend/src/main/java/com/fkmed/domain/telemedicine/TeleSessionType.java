package com.fkmed.domain.telemedicine;

/**
 * Whether a telemedicine session is an immediate Pronto Atendimento ({@code WALK_IN}) or a
 * scheduled teleconsultation ({@code SCHEDULED}) bridged from a {@code domain.appointment} booking
 * (SPEC-0010, DL-0018).
 *
 * <p>Kept as an enum, not registry data (invariant 7 / DECISIONS-BASELINE §0019): a closed,
 * two-value structural classification fixed by the product's two tele entry flows — not
 * runtime-editable reference data — and it drives wired branching (a walk-in carries triage and a
 * queue position; a scheduled session carries no position and links to its appointment). Same
 * keep-criterion as {@code domain.appointment.AppointmentType}.
 */
public enum TeleSessionType {
  WALK_IN,
  SCHEDULED
}
