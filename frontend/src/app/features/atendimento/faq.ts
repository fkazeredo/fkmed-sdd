import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AccordionModule } from 'primeng/accordion';
import { FaqCategory, FaqEntry, SupportApi } from './support.api';

interface CategoryOption {
  code: FaqCategory | null;
  labelKey: string;
}

const CATEGORY_OPTIONS: CategoryOption[] = [
  { code: null, labelKey: 'atendimento.faq.categoria.TODAS' },
  { code: 'REEMBOLSO', labelKey: 'atendimento.faq.categoria.REEMBOLSO' },
  { code: 'CARTEIRINHA', labelKey: 'atendimento.faq.categoria.CARTEIRINHA' },
  { code: 'AGENDAMENTO', labelKey: 'atendimento.faq.categoria.AGENDAMENTO' },
  { code: 'TELEMEDICINA', labelKey: 'atendimento.faq.categoria.TELEMEDICINA' },
  { code: 'BOLETOS', labelKey: 'atendimento.faq.categoria.BOLETOS' },
  { code: 'REDE', labelKey: 'atendimento.faq.categoria.REDE' },
];

/**
 * The FAQ screen (SPEC-0014 BR5/BR6): a real-time, case/accent-insensitive search (server-side,
 * AC1) combined with a category filter, over an accordion where opening a question closes the
 * previous one (AC3, mirroring Home's notices accordion — PrimeNG single-open by default).
 */
@Component({
  selector: 'app-faq',
  imports: [TranslatePipe, AccordionModule],
  templateUrl: './faq.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Faq {
  private readonly api = inject(SupportApi);

  protected readonly categories = CATEGORY_OPTIONS;
  protected readonly query = signal('');
  protected readonly category = signal<FaqCategory | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly entries = signal<FaqEntry[]>([]);
  protected readonly openId = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.load(this.query(), this.category());
    });
  }

  onQueryInput(value: string): void {
    this.query.set(value);
  }

  selectCategory(code: FaqCategory | null): void {
    this.category.set(code);
  }

  onOpenChange(value: string | number | string[] | number[] | null | undefined): void {
    this.openId.set(typeof value === 'string' ? value : null);
  }

  private load(query: string, category: FaqCategory | null): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.getFaq(query, category).subscribe({
      next: (list) => {
        this.entries.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.entries.set([]);
        this.errorKey.set('common.error');
        this.loading.set(false);
      },
    });
  }
}
