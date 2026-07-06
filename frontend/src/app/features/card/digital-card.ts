import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, effect, inject, OnDestroy, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { CardApi, CardResponse } from './card.api';

const COPY_CONFIRMATION_MS = 3000;

/**
 * The Digital Card screen (SPEC-0007): the beneficiary's official plan identification — visual
 * card + data sheet (BR1/BR2/BR9) for the **active** beneficiary, "Salvar Carteirinha" PDF
 * download (BR3), "Copiar número" quick action (BR6) and "Minhas Carteirinhas" (BR5, reusing the
 * SPEC-0003 accessible-beneficiaries context from Phase 1). Mirrors the Home screen's pattern
 * (SPEC-0005): an `effect` keyed off `BeneficiaryContextService.active()` reloads on every switch,
 * whether triggered from the shell selector (BR4) or from the "Minhas Carteirinhas" list below
 * (BR5→BR4). BR10: an inactive beneficiary's `card.unavailable` (409) renders a dedicated
 * "indisponível" state instead of the card — no retry, it is not a transient failure. BR7 (the
 * sensitive-data audit entry on a dependent's card) is a server-side effect of calling this
 * endpoint; nothing extra is required on the client.
 */
@Component({
  selector: 'app-digital-card',
  imports: [TranslatePipe],
  templateUrl: './digital-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DigitalCard implements OnDestroy {
  private readonly api = inject(CardApi);
  protected readonly context = inject(BeneficiaryContextService);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(true);
  readonly errorKey = signal<string | null>(null);
  readonly unavailable = signal(false);
  readonly card = signal<CardResponse | null>(null);

  readonly downloading = signal(false);
  readonly pdfErrorKey = signal<string | null>(null);

  readonly copied = signal(false);

  private currentBeneficiaryId: string | null = null;
  private copyTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    // Reacts to the active-beneficiary context (SPEC-0003 BR5) — including its initial load and
    // every subsequent switch, via the shell selector (BR4) or the "Minhas Carteirinhas" list
    // below (BR5).
    effect(() => {
      const activeId = this.context.active()?.beneficiaryId;
      if (activeId) {
        this.load(activeId);
      }
    });
  }

  ngOnDestroy(): void {
    this.clearCopyTimer();
  }

  load(beneficiaryId: string): void {
    this.currentBeneficiaryId = beneficiaryId;
    this.loading.set(true);
    this.errorKey.set(null);
    this.unavailable.set(false);
    this.pdfErrorKey.set(null);
    this.copied.set(false);
    this.api.getCard(beneficiaryId).subscribe({
      next: (response) => {
        this.card.set(response);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.card.set(null);
        this.loading.set(false);
        this.applyLoadError(error);
      },
    });
  }

  retry(): void {
    if (this.currentBeneficiaryId) {
      this.load(this.currentBeneficiaryId);
    }
  }

  selectBeneficiary(beneficiaryId: string): void {
    this.context.setActive(beneficiaryId);
  }

  savePdf(): void {
    const beneficiaryId = this.currentBeneficiaryId;
    if (!beneficiaryId || this.downloading()) {
      return;
    }
    this.downloading.set(true);
    this.pdfErrorKey.set(null);
    this.api.downloadPdf(beneficiaryId).subscribe({
      next: (blob) => {
        this.downloading.set(false);
        this.triggerDownload(blob, `carteirinha-${beneficiaryId}.pdf`);
      },
      error: () => {
        this.downloading.set(false);
        this.pdfErrorKey.set('carteirinha.erro.pdf');
      },
    });
  }

  /** BR6: copies exactly the 9-digit card number and confirms visually for a few seconds. */
  copyNumber(): void {
    const number = this.card()?.cardNumber;
    if (!number) {
      return;
    }
    void navigator.clipboard.writeText(number).then(() => {
      this.copied.set(true);
      this.clearCopyTimer();
      this.copyTimer = setTimeout(() => this.copied.set(false), COPY_CONFIRMATION_MS);
    });
  }

  /** Coverage is a backend registry code (e.g. ESTADUAL); the label always comes from the bundle.
   * BR2: both the card seal and the data-sheet field call this same helper on the same value. */
  coverageLabel(code: string): string {
    return this.translate.instant(`carteirinha.coverage.${code}`);
  }

  roleLabel(role: string): string {
    return this.translate.instant(`contexto.papel.${role}`);
  }

  private applyLoadError(error: HttpErrorResponse): void {
    switch (error.error?.code) {
      case 'card.unavailable':
        this.unavailable.set(true);
        break;
      case 'context.beneficiary-not-accessible':
        this.errorKey.set('contexto.erro.beneficiarioNaoAcessivel');
        break;
      default:
        this.errorKey.set('common.error');
    }
  }

  private triggerDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  private clearCopyTimer(): void {
    if (this.copyTimer) {
      clearTimeout(this.copyTimer);
      this.copyTimer = undefined;
    }
  }
}
