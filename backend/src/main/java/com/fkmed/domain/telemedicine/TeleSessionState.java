package com.fkmed.domain.telemedicine;

import java.util.Set;

/**
 * The telemedicine session lifecycle state machine (SPEC-0010 BR11).
 *
 * <p>Kept as an enum under invariant 7 / DECISIONS-BASELINE §0019: it is a lifecycle state machine
 * whose transitions the code enforces — not reference data. The edges are {@code EM_FILA ->
 * EM_ATENDIMENTO} (the turn is reached, BR8) or {@code EM_FILA -> ABANDONADA} (leave the queue,
 * BR5); and {@code EM_ATENDIMENTO -> ENCERRADA} (the professional closes, BR9) or {@code
 * EM_ATENDIMENTO -> ABANDONADA} (the 5-minute no-show after the turn, BR8/AC3 — the edge BR11's
 * happy-path list omits but AC3 requires). {@link #ENCERRADA} and {@link #ABANDONADA} are final.
 */
public enum TeleSessionState {
  EM_FILA,
  EM_ATENDIMENTO,
  ENCERRADA,
  ABANDONADA;

  private static final Set<TeleSessionState> ACTIVE = Set.of(EM_FILA, EM_ATENDIMENTO);

  /**
   * Whether the session is still live (in queue or being attended) rather than in a final state.
   */
  public boolean isActive() {
    return ACTIVE.contains(this);
  }

  /** Whether the session has reached a terminal state ({@code ENCERRADA}/{@code ABANDONADA}). */
  public boolean isFinal() {
    return !isActive();
  }

  /**
   * Whether a transition to {@code target} is allowed by the state machine (BR11 + the BR8 no-show
   * edge). Guards the turn/leave/close/no-show transitions against an already-closed session.
   */
  public boolean canTransitionTo(TeleSessionState target) {
    return switch (this) {
      case EM_FILA -> target == EM_ATENDIMENTO || target == ABANDONADA;
      case EM_ATENDIMENTO -> target == ENCERRADA || target == ABANDONADA;
      case ENCERRADA, ABANDONADA -> false;
    };
  }
}
