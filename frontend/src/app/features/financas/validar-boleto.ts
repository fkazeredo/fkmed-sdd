import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { FinanceApi, InvoiceValidation } from './finance.api';
import { FinanceDenied } from './finance-denied';
import { formatBrDate, formatBrl } from './finance-format';

/**
 * The antifraud boleto validator (SPEC-0013 BR4). Pastes a line → the server normalizes and requires
 * exactly 47 digits (422 finance.line-invalid-format → inline format hint), then answers AUTHENTIC
 * (shows competência/valor) or NOT_RECOGNIZED (the mandatory do-not-pay alert — the screen MUST
 * NEVER suggest paying an unrecognized boleto). Titular-only (BR1): a dependent sees the denial.
 */
@Component({
  selector: 'app-validar-boleto',
  imports: [FormsModule, TranslatePipe, FinanceDenied],
  templateUrl: './validar-boleto.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ValidarBoleto {
  private readonly api = inject(FinanceApi);
  private readonly context = inject(BeneficiaryContextService);

  protected readonly formatBrl = formatBrl;
  protected readonly formatBrDate = formatBrDate;

  protected line = '';
  protected readonly submitting = signal(false);
  protected readonly formatError = signal(false);
  protected readonly result = signal<InvoiceValidation | null>(null);

  protected readonly denied = computed(() => this.context.active()?.role === 'DEPENDENT');

  validate(): void {
    const value = this.line?.trim();
    if (!value || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.formatError.set(false);
    this.result.set(null);
    this.api.validate(value).subscribe({
      next: (verdict) => {
        this.submitting.set(false);
        this.result.set(verdict);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        if (error.error?.code === 'finance.line-invalid-format') {
          this.formatError.set(true);
        } else if (error.error?.code !== 'finance.titular-only') {
          this.result.set(null);
          this.formatError.set(true);
        }
      },
    });
  }
}
