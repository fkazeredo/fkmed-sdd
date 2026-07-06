package com.fkmed.domain.guides;

/**
 * The authorization-guide lifecycle state machine (SPEC-0012 BR6).
 *
 * <p>Kept as an enum under invariant 7 / DECISIONS-BASELINE §0019: it is a lifecycle state machine
 * whose transitions the code enforces — not reference data — mirroring {@code
 * domain.telemedicine.TeleSessionState}'s keep criterion.
 *
 * <p>Edges: {@link #EM_ANALISE} may move to {@link #AUTORIZADA}, {@link #PARCIALMENTE_AUTORIZADA},
 * {@link #NEGADA} or {@link #CANCELADA} (the operator's authorize/ partially-authorize/deny/cancel
 * actions); {@link #AUTORIZADA} and {@link #PARCIALMENTE_AUTORIZADA} may move to {@link #EXECUTADA}
 * or {@link #CANCELADA}. {@link #NEGADA}, {@link #CANCELADA} and {@link #EXECUTADA} are final.
 */
public enum GuideStatus {
  EM_ANALISE,
  AUTORIZADA,
  PARCIALMENTE_AUTORIZADA,
  NEGADA,
  CANCELADA,
  EXECUTADA;

  /** Whether a transition from this status to {@code target} is allowed by the state machine. */
  public boolean canTransitionTo(GuideStatus target) {
    return switch (this) {
      case EM_ANALISE ->
          target == AUTORIZADA
              || target == PARCIALMENTE_AUTORIZADA
              || target == NEGADA
              || target == CANCELADA;
      case AUTORIZADA, PARCIALMENTE_AUTORIZADA -> target == EXECUTADA || target == CANCELADA;
      case NEGADA, CANCELADA, EXECUTADA -> false;
    };
  }

  /** Whether the guide has reached a terminal state (no further transition is possible). */
  public boolean isFinal() {
    return this == NEGADA || this == CANCELADA || this == EXECUTADA;
  }
}
