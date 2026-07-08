package com.fkmed.domain.support;

/**
 * Lifecycle of a Libras service request (SPEC-0014 BR4) — a state machine, so per
 * DECISIONS-BASELINE §0019 it is a genuine enum rather than a registry code. {@code ATTENDED} is a
 * future operator-side transition: this POC never sets it (the videocall itself is conducted by the
 * operator's team, out of scope — SPEC-0014 §Out of Scope).
 */
public enum LibrasRequestSituation {
  REGISTERED,
  ATTENDED
}
