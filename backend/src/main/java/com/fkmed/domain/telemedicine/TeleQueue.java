package com.fkmed.domain.telemedicine;

/**
 * Pure queue arithmetic for the Pronto Atendimento fila (SPEC-0010 BR5/BR6): the beneficiary's
 * position from the number of sessions ahead and the estimated wait from the position.
 *
 * <p>Isolated as pure functions (no persistence) so the position/ETA rule is unit-testable and its
 * mutants are killable independently of the repository that supplies the "ahead" count.
 */
final class TeleQueue {

  /** Estimated minutes of wait attributed to each place in line (SPEC-0010 example: pos 4 → 12). */
  static final int MINUTES_PER_POSITION = 3;

  private TeleQueue() {}

  /** The 1-based position given the number of queued sessions strictly ahead. */
  static int positionFrom(long ahead) {
    return (int) ahead + 1;
  }

  /** The estimated wait in minutes for a given 1-based position. */
  static int etaMinutes(int position) {
    return position * MINUTES_PER_POSITION;
  }
}
