import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { CopayPeriod, CopayStatement, FinanceApi } from './finance.api';
import { FinanceDenied } from './finance-denied';
import { formatBrDate, formatBrl } from './finance-format';

const ALL = 'all';

/**
 * The copay statement (SPEC-0013 BR5): filters by period (current month, last 3 months, custom
 * range) and by beneficiary (whole family or one member); a table with date/procedure/provider/
 * beneficiary/valor and the total of the filtered period, recomputed on every filter change; an
 * empty state. Titular-only (BR1) but covers the whole family's copay. A CUSTOM range applies only
 * once both dates are set (the "Aplicar filtro" action); the named periods apply immediately.
 */
@Component({
  selector: 'app-coparticipacao',
  imports: [FormsModule, TranslatePipe, FinanceDenied],
  templateUrl: './coparticipacao.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Coparticipacao {
  private readonly api = inject(FinanceApi);
  private readonly context = inject(BeneficiaryContextService);

  protected readonly formatBrl = formatBrl;
  protected readonly formatBrDate = formatBrDate;
  protected readonly ALL = ALL;

  protected readonly period = signal<CopayPeriod>('CURRENT_MONTH');
  protected readonly beneficiaryId = signal<string>(ALL);
  protected readonly customFrom = signal<string>('');
  protected readonly customTo = signal<string>('');

  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly statement = signal<CopayStatement | null>(null);

  protected readonly family = computed(() => this.context.accessible());
  protected readonly denied = computed(() => this.context.active()?.role === 'DEPENDENT');

  constructor() {
    // Recompute on every named-period / beneficiary change (CUSTOM is applied explicitly).
    effect(() => {
      const period = this.period();
      const beneficiary = this.beneficiaryId();
      const role = this.context.active()?.role;
      if (role === 'TITULAR' && period !== 'CUSTOM') {
        this.load(period, beneficiary, undefined, undefined);
      }
    });
  }

  onPeriodChange(period: CopayPeriod): void {
    this.period.set(period);
  }

  onBeneficiaryChange(beneficiaryId: string): void {
    this.beneficiaryId.set(beneficiaryId);
  }

  applyCustomRange(): void {
    const from = this.customFrom();
    const to = this.customTo();
    if (!from || !to) {
      return;
    }
    this.load('CUSTOM', this.beneficiaryId(), from, to);
  }

  private load(period: CopayPeriod, beneficiaryId: string, from?: string, to?: string): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.api
      .getCopay({
        period,
        from,
        to,
        beneficiaryId: beneficiaryId === ALL ? undefined : beneficiaryId,
      })
      .subscribe({
        next: (statement) => {
          this.statement.set(statement);
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.statement.set(null);
          this.loading.set(false);
          this.errorKey.set(error.error?.code === 'finance.titular-only' ? null : 'common.error');
        },
      });
  }
}
