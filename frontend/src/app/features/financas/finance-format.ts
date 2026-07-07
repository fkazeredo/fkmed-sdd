/** pt-BR money/date formatting for the Finanças screens (SPEC-0013). Pure, deterministic. */

const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/** Formats a number as "R$ 1.234,56". */
export function formatBrl(value: number | undefined | null): string {
  return BRL.format(value ?? 0);
}

/** Formats an ISO date (yyyy-MM-dd) as "dd/MM/yyyy", without any timezone shift. */
export function formatBrDate(iso: string | undefined | null): string {
  if (!iso) {
    return '';
  }
  const [year, month, day] = iso.split('-');
  return `${day}/${month}/${year}`;
}
