const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function formatBrl(value: number | undefined | null): string {
  return BRL.format(value ?? 0);
}

export function formatBrDate(iso: string | undefined | null): string {
  if (!iso) {
    return '';
  }
  const [date] = iso.split('T');
  const [year, month, day] = date.split('-');
  return `${day}/${month}/${year}`;
}

export function statusBadge(status: string): string {
  return (
    {
      EM_ANALISE: 'bg-slate-100 text-slate-700',
      PROCESSAMENTO: 'bg-blue-100 text-blue-700',
      PENDENTE_DOCUMENTACAO: 'bg-amber-100 text-amber-700',
      APROVADO: 'bg-emerald-100 text-emerald-700',
      PAGO: 'bg-teal-100 text-teal-700',
      PAGAMENTO_NAO_EFETUADO: 'bg-red-100 text-red-700',
      NEGADO: 'bg-red-100 text-red-700',
      CANCELADO: 'bg-slate-100 text-slate-500',
      CONCLUIDA: 'bg-teal-100 text-teal-700',
    }[status] ?? 'bg-slate-100 text-slate-700'
  );
}

