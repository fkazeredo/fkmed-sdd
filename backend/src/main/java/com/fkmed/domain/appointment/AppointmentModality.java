package com.fkmed.domain.appointment;

/**
 * Whether an appointment is served in person or by telemedicine (SPEC-0010 BR14, DL-0018).
 *
 * <p>Kept as an enum, not registry data (invariant 7 / DECISIONS-BASELINE §0019): a closed,
 * two-value structural classification fixed by the product — not runtime-editable reference data —
 * added as an additive orthogonal flag rather than a new {@link AppointmentType} value, so the
 * booking/cancel/reschedule/protocol machinery is fully inherited. It drives wired branching (only
 * a {@code TELEMEDICINA} appointment exposes the "Entrar na consulta" join window). Same
 * keep-criterion as {@link AppointmentType}.
 */
public enum AppointmentModality {
  PRESENCIAL,
  TELEMEDICINA
}
