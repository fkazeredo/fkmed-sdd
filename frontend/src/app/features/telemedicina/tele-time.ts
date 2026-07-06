import { SymptomDuration } from './tele.api';

/** BR14: the join button opens 10 minutes before the slot. */
export const JOIN_WINDOW_OPEN_MINUTES_BEFORE = 10;

/** BR14 "until its end": tele slots are 30-min consultations (same slot grid as SPEC-0009, :00/:30).
 * The backend is authoritative — a click outside the window still gets `409 tele.join-window-closed`;
 * this bound only governs the client-side enablement UX. Assumption flagged for confirmation. */
export const TELE_SLOT_DURATION_MINUTES = 30;

/** i18n key of each duration option (BR2 fixed list), rendered in the shown order. */
export const SYMPTOM_DURATIONS: readonly SymptomDuration[] = ['HORAS', 'D1_3', 'D3_MAIS', 'SEMANA_MAIS'];

/** TZ-safe parse of a `YYYY-MM-DDTHH:mm` local datetime (no `Date('...')` UTC shift — same rationale
 * as slot-picker's `formatDayLabel`). */
export function parseLocalDateTime(iso: string): Date {
  const [datePart, timePart] = iso.split('T');
  const [year, month, day] = datePart.split('-').map(Number);
  const [hour, minute] = (timePart ?? '00:00').split(':').map(Number);
  return new Date(year, month - 1, day, hour, minute);
}

/** BR14/AC6: is "Entrar na consulta" enabled now — from 10 min before the slot until its end. */
export function isJoinWindowOpen(scheduledAtIso: string, now: Date): boolean {
  const start = parseLocalDateTime(scheduledAtIso).getTime();
  const openFrom = start - JOIN_WINDOW_OPEN_MINUTES_BEFORE * 60_000;
  const closeAt = start + TELE_SLOT_DURATION_MINUTES * 60_000;
  const at = now.getTime();
  return at >= openFrom && at <= closeAt;
}

/** BR9: running duration `H:MM:SS` (hours dropped when zero) from a start instant to `now`. Negative
 * spans (clock skew) clamp to zero. */
export function formatElapsed(startIso: string, now: Date): string {
  // room.startedAt is a UTC Instant (…Z) — parse it as a real instant (not wall-clock) so the live
  // running duration does not drift by the timezone offset. A bare local datetime also parses fine.
  const elapsedMs = Math.max(0, now.getTime() - new Date(startIso).getTime());
  const totalSeconds = Math.floor(elapsedMs / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const mm = String(minutes).padStart(2, '0');
  const ss = String(seconds).padStart(2, '0');
  return hours > 0 ? `${hours}:${mm}:${ss}` : `${mm}:${ss}`;
}
