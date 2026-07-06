import { GuideStatus } from './guias.api';

/** TZ-safe date/date-time → `dd/mm/aaaa` (string split, no `Date('...')` parsing that shifts by
 * timezone — mirrors `formatBrDate` in features/minha-saude/document-format.ts). Accepts a bare
 * `yyyy-MM-dd` or a `yyyy-MM-ddTHH:mm...` value; only the date part is rendered. */
export function formatBrDate(isoDate: string): string {
  const [datePart] = isoDate.split('T');
  const [year, month, day] = datePart.split('-');
  return `${day}/${month}/${year}`;
}

/** BR2/BR6: a distinct visual per status (Tailwind classes), keyed by the state machine. */
export const GUIDE_STATUS_BADGE: Record<GuideStatus, string> = {
  EM_ANALISE: 'bg-amber-50 text-amber-700',
  AUTORIZADA: 'bg-emerald-50 text-emerald-700',
  PARCIALMENTE_AUTORIZADA: 'bg-teal-50 text-teal-700',
  NEGADA: 'bg-red-50 text-red-700',
  CANCELADA: 'bg-slate-100 text-slate-600',
  EXECUTADA: 'bg-blue-50 text-blue-700',
};

/** BR5: only these two statuses show the authorization password + validity. */
export function showsAuthPassword(status: GuideStatus): boolean {
  return status === 'AUTORIZADA' || status === 'PARCIALMENTE_AUTORIZADA';
}
