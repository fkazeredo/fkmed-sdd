/** TZ-safe `yyyy-MM-dd` → `dd/mm/aaaa` (string split, no `Date('...')` parsing that shifts by
 * timezone — mirrors `formatDayLabel` in features/agendamento/slot-picker.ts). */
export function formatBrDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-');
  return `${day}/${month}/${year}`;
}

export type ValidityStatus = 'none' | 'valid' | 'expired';

/** BR4/BR5: a `null` validUntil (sick note) has no badge at all; otherwise "Expirado" or "Válido
 * até dd/mm/aaaa" depending on `expired`. */
export function validityStatus(document: { validUntil: string | null; expired: boolean }): ValidityStatus {
  if (document.validUntil === null) {
    return 'none';
  }
  return document.expired ? 'expired' : 'valid';
}

/** BR2: most-recent-first — used to re-sort the merged result of the two API calls behind the
 * combined "Receituários/Atestados" category (PRESCRIPTION + SICK_NOTE). Returns a new array. */
export function sortByIssuedAtDesc<T extends { issuedAt: string }>(items: T[]): T[] {
  return [...items].sort((a, b) => b.issuedAt.localeCompare(a.issuedAt));
}
