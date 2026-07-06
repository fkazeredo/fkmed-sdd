import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { BeneficiarySummary, BeneficiarySummaryApi } from '../../core/context/beneficiary-summary.api';
import {
  GuideCard,
  GuideListFilters,
  GuidePeriodOption,
  GuideStatus,
  GuidesApi,
  TokenResponse,
} from './guias.api';
import { formatBrDate, GUIDE_STATUS_BADGE } from './guide-format';
import { formatCountdown, isTokenExpired, remainingSeconds, TOKEN_TICK_MS } from './token-time';

const COPY_CONFIRMATION_MS = 3000;
const GUIDE_STATUSES: GuideStatus[] = [
  'EM_ANALISE',
  'AUTORIZADA',
  'PARCIALMENTE_AUTORIZADA',
  'NEGADA',
  'CANCELADA',
  'EXECUTADA',
];

/**
 * Guias e Token (SPEC-0012 BR1): ONE screen — header actions Atualizar/Filtrar, the plan strip,
 * the beneficiary picker and the two sections Guias (BR2/BR3/BR4) and Token (BR9/BR10/BR11). An
 * `effect` keyed off `BeneficiaryContextService.active()` reloads all three (plan strip, guide
 * list, current token) on every switch — whether triggered from the shell selector or the on-page
 * picker below (mirrors DigitalCard/Home's established pattern).
 */
@Component({
  selector: 'app-guias-hub',
  imports: [FormsModule, RouterLink, TranslatePipe],
  templateUrl: './guias-hub.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuiasHub implements OnDestroy {
  private readonly api = inject(GuidesApi);
  private readonly summaryApi = inject(BeneficiarySummaryApi);
  protected readonly context = inject(BeneficiaryContextService);

  protected readonly guideStatuses = GUIDE_STATUSES;
  protected readonly badgeClass = GUIDE_STATUS_BADGE;
  protected readonly formatBrDate = formatBrDate;

  // Plan strip (BR1).
  protected readonly summary = signal<BeneficiarySummary | null>(null);

  // Guias section (BR1-BR4).
  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly guides = signal<GuideCard[]>([]);
  protected readonly showFilters = signal(false);
  protected readonly statusFilter = signal<GuideStatus | 'all'>('all');
  protected readonly period = signal<GuidePeriodOption>('LAST_90D');

  // Token section (BR9/BR10/BR11).
  protected readonly tokenLoading = signal(true);
  protected readonly token = signal<TokenResponse | null>(null);
  protected readonly tokenErrorKey = signal<string | null>(null);
  protected readonly generating = signal(false);
  protected readonly copied = signal(false);
  protected readonly now = signal(new Date());

  /** BR10: the code stops being valid the instant the countdown reaches 00:00. */
  protected readonly tokenExpired = computed(() => {
    const current = this.token();
    return current ? isTokenExpired(current.expiresAt, this.now()) : false;
  });
  /** BR9: the visible mm:ss countdown, recomputed every tick. */
  protected readonly countdown = computed(() => {
    const current = this.token();
    return current ? formatCountdown(remainingSeconds(current.expiresAt, this.now())) : '00:00';
  });

  private ticker?: ReturnType<typeof setInterval>;
  private copyTimer?: ReturnType<typeof setTimeout>;
  private currentBeneficiaryId: string | null = null;

  constructor() {
    effect(() => {
      const activeId = this.context.active()?.beneficiaryId;
      if (activeId) {
        this.currentBeneficiaryId = activeId;
        this.loadSummary(activeId);
        this.loadGuides();
        this.loadCurrentToken(activeId);
      }
    });
    this.ticker = setInterval(() => this.now.set(new Date()), TOKEN_TICK_MS);
  }

  ngOnDestroy(): void {
    if (this.ticker) {
      clearInterval(this.ticker);
    }
    this.clearCopyTimer();
  }

  selectBeneficiary(beneficiaryId: string): void {
    this.context.setActive(beneficiaryId);
  }

  toggleFilters(): void {
    this.showFilters.update((value) => !value);
  }

  onStatusChange(value: GuideStatus | 'all'): void {
    this.statusFilter.set(value);
    this.loadGuides();
  }

  /** BR2: switching the period re-queries immediately (the backend's `GuidePeriod` enum — no
   * custom range). */
  onPeriodChange(value: GuidePeriodOption): void {
    this.period.set(value);
    this.loadGuides();
  }

  /** BR4: Atualizar (header action) and "Atualizar informações" (empty state) both call this. */
  refresh(): void {
    this.loadGuides();
  }

  loadGuides(): void {
    const beneficiaryId = this.currentBeneficiaryId;
    if (!beneficiaryId) {
      return;
    }
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.getGuides(this.buildFilters(beneficiaryId)).subscribe({
      next: (items) => {
        this.guides.set(items);
        this.loading.set(false);
      },
      error: () => {
        this.errorKey.set('common.error');
        this.loading.set(false);
      },
    });
  }

  /** BR9: generates a token — the initial one, or a replacement while one is active/expired
   * (the backend invalidates the previous immediately). */
  generateToken(): void {
    const beneficiaryId = this.currentBeneficiaryId;
    if (!beneficiaryId || this.generating()) {
      return;
    }
    this.generating.set(true);
    this.tokenErrorKey.set(null);
    this.api.generateToken(beneficiaryId).subscribe({
      next: (response) => {
        this.generating.set(false);
        this.token.set(response);
        this.copied.set(false);
      },
      error: () => {
        this.generating.set(false);
        this.tokenErrorKey.set('common.error');
      },
    });
  }

  /** BR11: copies exactly the 6-digit code — never available once expired (BR10). */
  copyToken(): void {
    const code = this.token()?.code;
    if (!code || this.tokenExpired()) {
      return;
    }
    void navigator.clipboard.writeText(code).then(() => {
      this.copied.set(true);
      this.clearCopyTimer();
      this.copyTimer = setTimeout(() => this.copied.set(false), COPY_CONFIRMATION_MS);
    });
  }

  private loadSummary(beneficiaryId: string): void {
    this.summaryApi.getBeneficiary(beneficiaryId).subscribe({
      next: (response) => this.summary.set(response),
      error: () => this.summary.set(null),
    });
  }

  /** BR9/BR10: resumes an active token's countdown on load; a 404 `token.none-active` means no
   * active token — not an error (spec §Error Behavior), so it is silently mapped to `null`. */
  private loadCurrentToken(beneficiaryId: string): void {
    this.tokenLoading.set(true);
    this.tokenErrorKey.set(null);
    this.api.getCurrentToken(beneficiaryId).subscribe({
      next: (response) => {
        this.token.set(response);
        this.tokenLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.tokenLoading.set(false);
        if (error.status === 404) {
          this.token.set(null);
        } else {
          this.tokenErrorKey.set('common.error');
        }
      },
    });
  }

  private buildFilters(beneficiaryId: string): GuideListFilters {
    const status = this.statusFilter();
    const base: GuideListFilters = { beneficiaryId, period: this.period() };
    if (status !== 'all') {
      base.status = status;
    }
    return base;
  }

  private clearCopyTimer(): void {
    if (this.copyTimer) {
      clearTimeout(this.copyTimer);
      this.copyTimer = undefined;
    }
  }
}
