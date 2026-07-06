package com.fkmed.domain.support;

/**
 * Lifecycle of a Central de Libras service request (SPEC-0014 BR4).
 *
 * <p>Kept as an enum, a state machine per DECISIONS-BASELINE §0019: {@code REGISTERED} is the only
 * state this slice ever persists (a beneficiary registration); {@code ATTENDED} is a future
 * operator-side transition (an SPEC-0018 operator-simulation seam, out of scope here) marking that
 * the videocall took place.
 */
public enum LibrasSituation {
  REGISTERED,
  ATTENDED
}
