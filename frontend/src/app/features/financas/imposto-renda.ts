import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { FinanceApi, StatementYear } from './finance.api';
import { FinanceDenied } from './finance-denied';
import { downloadBlob } from './download';

/**
 * The income-tax (IR) statements (SPEC-0013 BR6): the base years with payments, each offering its
 * demonstrativo PDF (contract, 12 monthly amounts, annual total). Titular-only (BR1). An empty list
 * shows a guidance state (no base year has payments yet).
 */
@Component({
  selector: 'app-imposto-renda',
  imports: [TranslatePipe, FinanceDenied],
  templateUrl: './imposto-renda.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImpostoRenda {
  private readonly api = inject(FinanceApi);
  private readonly context = inject(BeneficiaryContextService);

  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly years = signal<StatementYear[]>([]);
  protected readonly downloadingYear = signal<number | null>(null);
  protected readonly pdfErrorKey = signal<string | null>(null);

  protected readonly denied = computed(() => this.context.active()?.role === 'DEPENDENT');

  constructor() {
    effect(() => {
      if (this.context.active()?.role === 'TITULAR') {
        this.load();
      }
    });
  }

  load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.getTaxStatements().subscribe({
      next: (years) => {
        this.years.set(years);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.years.set([]);
        this.loading.set(false);
        this.errorKey.set(error.error?.code === 'finance.titular-only' ? null : 'common.error');
      },
    });
  }

  download(year: number): void {
    if (this.downloadingYear() !== null) {
      return;
    }
    this.downloadingYear.set(year);
    this.pdfErrorKey.set(null);
    this.api.downloadTaxStatementPdf(year).subscribe({
      next: (blob) => {
        this.downloadingYear.set(null);
        downloadBlob(blob, `ir-${year}.pdf`);
      },
      error: () => {
        this.downloadingYear.set(null);
        this.pdfErrorKey.set('financas.erro.pdf');
      },
    });
  }
}
