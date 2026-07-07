import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { FinanceApi, InvoiceDetail } from './finance.api';
import { FinanceDenied } from './finance-denied';
import { downloadBlob } from './download';
import { formatBrDate, formatBrl } from './finance-format';

const COPY_CONFIRMATION_MS = 3000;

/**
 * The boleto detail (SPEC-0013 BR3): competência/vencimento/valor (+ valor atualizado when
 * overdue), "Copiar linha digitável" (exactly the 47 digits + confirmation), "PIX copia-e-cola"
 * (copies the PIX code + confirmation — code exposure only, no online payment, BR8) and "Baixar 2ª
 * via (PDF)" (generatable in any state; a paid boleto's PDF carries the "PAGO" watermark).
 * Titular-only (BR1): a dependent sees the friendly denial. A missing/out-of-contract id renders a
 * "não encontrado" state (404 finance.invoice-not-found, existence not revealed).
 */
@Component({
  selector: 'app-boleto-detail',
  imports: [TranslatePipe, FinanceDenied],
  templateUrl: './boleto-detail.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BoletoDetail implements OnInit, OnDestroy {
  private readonly api = inject(FinanceApi);
  private readonly route = inject(ActivatedRoute);
  private readonly context = inject(BeneficiaryContextService);

  protected readonly formatBrl = formatBrl;
  protected readonly formatBrDate = formatBrDate;

  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly invoice = signal<InvoiceDetail | null>(null);

  protected readonly copiedLine = signal(false);
  protected readonly copiedPix = signal(false);
  protected readonly downloading = signal(false);
  protected readonly pdfErrorKey = signal<string | null>(null);

  protected readonly denied = computed(() => this.context.active()?.role === 'DEPENDENT');

  private lineTimer: ReturnType<typeof setTimeout> | undefined;
  private pixTimer: ReturnType<typeof setTimeout> | undefined;

  ngOnInit(): void {
    // BR1: a dependent sees the denial and never queries the contract's boleto.
    if (!this.denied()) {
      this.load();
    }
  }

  ngOnDestroy(): void {
    clearTimeout(this.lineTimer);
    clearTimeout(this.pixTimer);
  }

  load(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }
    this.loading.set(true);
    this.notFound.set(false);
    this.errorKey.set(null);
    this.api.getInvoice(id).subscribe({
      next: (detail) => {
        this.invoice.set(detail);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        if (error.error?.code === 'finance.invoice-not-found') {
          this.notFound.set(true);
        } else if (error.error?.code !== 'finance.titular-only') {
          this.errorKey.set('common.error');
        }
      },
    });
  }

  /** BR3: copies exactly the 47 digits of the digitable line and confirms briefly. */
  copyLine(): void {
    const line = this.invoice()?.digitableLine;
    if (!line) {
      return;
    }
    void navigator.clipboard.writeText(line).then(() => {
      this.copiedLine.set(true);
      clearTimeout(this.lineTimer);
      this.lineTimer = setTimeout(() => this.copiedLine.set(false), COPY_CONFIRMATION_MS);
    });
  }

  /** BR3: copies the PIX copia-e-cola code (exposure only, no online payment) and confirms briefly. */
  copyPix(): void {
    const pix = this.invoice()?.pixCode;
    if (!pix) {
      return;
    }
    void navigator.clipboard.writeText(pix).then(() => {
      this.copiedPix.set(true);
      clearTimeout(this.pixTimer);
      this.pixTimer = setTimeout(() => this.copiedPix.set(false), COPY_CONFIRMATION_MS);
    });
  }

  downloadPdf(): void {
    const invoice = this.invoice();
    if (!invoice || this.downloading()) {
      return;
    }
    this.downloading.set(true);
    this.pdfErrorKey.set(null);
    this.api.downloadInvoicePdf(invoice.id).subscribe({
      next: (blob) => {
        this.downloading.set(false);
        downloadBlob(blob, `boleto-${invoice.id}.pdf`);
      },
      error: () => {
        this.downloading.set(false);
        this.pdfErrorKey.set('financas.erro.pdf');
      },
    });
  }
}
