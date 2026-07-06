/** Countdown tick cadence (BR9: a visible 10:00 countdown, driven client-side by `expiresAt`). */
export const TOKEN_TICK_MS = 1000;

/** Seconds left until `expiresAtIso` from `now` — never negative (clamps to zero once expired). */
export function remainingSeconds(expiresAtIso: string, now: Date): number {
  const ms = new Date(expiresAtIso).getTime() - now.getTime();
  return Math.max(0, Math.ceil(ms / 1000));
}

/** BR9: renders a total-seconds count as `mm:ss` (e.g. 600 → "10:00", 0 → "00:00"). */
export function formatCountdown(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

/** BR10: the code stops being valid the instant the countdown reaches zero. */
export function isTokenExpired(expiresAtIso: string, now: Date): boolean {
  return remainingSeconds(expiresAtIso, now) <= 0;
}
