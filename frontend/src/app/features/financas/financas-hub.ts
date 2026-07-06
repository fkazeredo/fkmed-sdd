import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { FinanceApi, InvoiceSummary, InvoiceTab } from './finance.api';
import { FinanceDenied } from './finance-denied';
import { formatBrDate, formatBrl } from './finance-format';

interface FinanceLink {
  key: string;
  icon: string;
  labelKey: string;
  routerLink: string;
}

const FINANCE_LINKS: FinanceLink[] = [
  { key: 'validar', icon: 'pi pi-shield', labelKey: 'financas.hub.validar', routerLink: '/financas/validar' },
  { key: 'coparticipacao', icon: 'pi pi-list', labelKey: 'financas.hub.coparticipacao', routerLink: '/financas/coparticipacao' },
  { key: 'impostoRenda', icon: 'pi pi-file', labelKey: 'financas.hub.impostoRenda', routerLink: '/financas/imposto-renda' },
  { key: 'quitacao', icon: 'pi pi-check-circle', labelKey: 'financas.hub.quitacao', routerLink: '/financas/quitacao' },
];

/**
 * The Finanças hub (SPEC-0013): the invoice tabs (Em aberto = open + overdue, Pagos, BR2) plus the
 * entry cards to the other finance features. Titular-only (BR1): when the active beneficiary is a
 * dependent, the whole screen renders the friendly denial ({@link FinanceDenied}) — the nav entry
 * is already hidden by the shell. An overdue invoice is highlighted with the channels guidance and
 * its valor atualizado breakdown (BR2); no online payment is offered (BR8).
 */
@Component({
  selector: 'app-financas-hub',
  imports: [RouterLink, TranslatePipe, FinanceDenied],
  templateUrl: './financas-hub.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FinancasHub {
  private readonly api = inject(FinanceApi);
  private readonly context = inject(BeneficiaryContextService);

  protected readonly links = FINANCE_LINKS;
  protected readonly formatBrl = formatBrl;
  protected readonly formatBrDate = formatBrDate;

  protected readonly tab = signal<InvoiceTab>('OPEN');
  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly invoices = signal<InvoiceSummary[]>([]);

  /** BR1: a dependent active beneficiary is denied the whole finance area. */
  protected readonly denied = computed(() => this.context.active()?.role === 'DEPENDENT');

  constructor() {
    // Load the active tab whenever the caller is the titular and the tab changes.
    effect(() => {
      const role = this.context.active()?.role;
      const tab = this.tab();
      if (role === 'TITULAR') {
        this.load(tab);
      }
    });
  }

  selectTab(tab: InvoiceTab): void {
    if (this.tab() !== tab) {
      this.tab.set(tab);
    }
  }

  load(tab: InvoiceTab): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.getInvoices(tab).subscribe({
      next: (list) => {
        this.invoices.set(list);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.invoices.set([]);
        this.loading.set(false);
        this.errorKey.set(error.error?.code === 'finance.titular-only' ? null : 'common.error');
      },
    });
  }

  retry(): void {
    this.load(this.tab());
  }
}
