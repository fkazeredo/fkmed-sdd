import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AccordionModule } from 'primeng/accordion';
import { CardModule } from 'primeng/card';
import { CarouselModule } from 'primeng/carousel';
import { DialogModule } from 'primeng/dialog';
import { AvatarStateService } from '../../core/context/avatar-state.service';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { BeneficiarySummary, BeneficiarySummaryApi } from '../../core/context/beneficiary-summary.api';
import { HomeApi, HomeBanner, HomeNotice } from './home.api';

interface QuickAccessShortcut {
  key: string;
  icon: string;
  labelKey: string;
  enabled: boolean;
}

/** BR3: fixed order, 9 shortcuts. Only Reconhecimento Facial is "enabled" in this phase — it has
 * no destination screen at all (mobile-only forever), so it opens an informational dialog
 * instead of navigating. Every other shortcut's destination module is not yet delivered
 * (phased-delivery note) and renders disabled with the "em breve" hint (BR4). */
const QUICK_ACCESS_SHORTCUTS: QuickAccessShortcut[] = [
  {
    key: 'reconhecimentoFacial',
    icon: 'pi pi-face-smile',
    labelKey: 'home.acessoRapido.reconhecimentoFacial',
    enabled: true,
  },
  { key: 'guiasTokens', icon: 'pi pi-file', labelKey: 'home.acessoRapido.guiasTokens', enabled: false },
  {
    key: 'redeCredenciada',
    icon: 'pi pi-map',
    labelKey: 'home.acessoRapido.redeCredenciada',
    enabled: false,
  },
  { key: 'telemedicina', icon: 'pi pi-video', labelKey: 'home.acessoRapido.telemedicina', enabled: false },
  {
    key: 'agendamento',
    icon: 'pi pi-calendar',
    labelKey: 'home.acessoRapido.agendamento',
    enabled: false,
  },
  { key: 'carteirinha', icon: 'pi pi-id-card', labelKey: 'home.acessoRapido.carteirinha', enabled: false },
  { key: 'minhaSaude', icon: 'pi pi-heart', labelKey: 'home.acessoRapido.minhaSaude', enabled: false },
  {
    key: 'canaisAtendimento',
    icon: 'pi pi-headphones',
    labelKey: 'home.acessoRapido.canaisAtendimento',
    enabled: false,
  },
  {
    key: 'alteracaoCadastral',
    icon: 'pi pi-user-edit',
    labelKey: 'home.acessoRapido.alteracaoCadastral',
    enabled: false,
  },
];

const BANNER_ROTATION_MS = 6000;

/**
 * The Home screen (SPEC-0005): the daily entry point after login — the active beneficiary's
 * card (BR1/BR2), the fixed 9-shortcut quick-access carousel (BR3/BR4/BR5), operator banners
 * (BR6) and notices (BR7). Phased delivery (owner decision, 2026-07-04, recorded in the spec):
 * every shortcut/banner whose destination module is not yet built renders disabled with an "em
 * breve" hint instead of navigating to a non-existent route (BR4, extended to banner buttons);
 * Reconhecimento Facial and the avatar are the two exceptions that open an informational dialog
 * instead of being disabled — neither has ever had a destination in this phase (mobile-only /
 * Profile is SPEC-0006).
 */
@Component({
  selector: 'app-home',
  imports: [TranslatePipe, CardModule, CarouselModule, AccordionModule, DialogModule],
  templateUrl: './home.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home implements OnInit, OnDestroy {
  private readonly homeApi = inject(HomeApi);
  private readonly beneficiaryApi = inject(BeneficiarySummaryApi);
  protected readonly context = inject(BeneficiaryContextService);
  private readonly avatar = inject(AvatarStateService);

  protected readonly shortcuts = QUICK_ACCESS_SHORTCUTS;

  // Card (BR1/BR2, AC1/AC5): keyed off the active beneficiary; re-fetches on every switch.
  readonly cardLoading = signal(true);
  readonly cardError = signal(false);
  readonly summary = signal<BeneficiarySummary | null>(null);

  // SPEC-0006 BR3: the avatar reflects the shared avatar state (a Bearer-authenticated blob), so a
  // photo changed in the Perfil area shows here without a new login; null → the initial placeholder.
  readonly avatarUrl = computed(() => this.avatar.avatarUrl(this.context.active()?.beneficiaryId));

  // Content (BR6/BR7/BR8).
  private readonly banners = signal<HomeBanner[]>([]);
  private readonly notices = signal<HomeNotice[]>([]);
  readonly sortedBanners = computed(() => [...this.banners()].sort((a, b) => a.order - b.order));
  readonly sortedNotices = computed(() => [...this.notices()].sort((a, b) => a.order - b.order));

  // Banner rotation (BR6): a self-owned timer so pause-on-hover/focus is simple and reliable —
  // the PrimeNG carousel stays in "controlled page" mode (drag/indicators/nav buttons still work
  // natively; the rotation cadence itself is ours).
  readonly bannerPage = signal(0);
  private bannerTimer: ReturnType<typeof setInterval> | undefined;
  private bannerPaused = false;

  // Notices accordion (BR7): single-open (default PrimeNG Accordion behavior), defaults to the
  // top-priority (first) notice once content loads.
  readonly openNoticeValue = signal<number | null>(null);

  // "Em breve" informational dialog (avatar click / Reconhecimento Facial).
  readonly dialogMessageKey = signal<string | null>(null);

  constructor() {
    // Reacts to the active-beneficiary context (SPEC-0003 BR5) — including its initial load and
    // every subsequent switch via the shell selector (AC5).
    effect(() => {
      const activeId = this.context.active()?.beneficiaryId;
      if (activeId) {
        this.loadSummary(activeId);
      }
    });
  }

  ngOnInit(): void {
    this.loadContent();
  }

  ngOnDestroy(): void {
    this.stopBannerRotation();
  }

  loadSummary(beneficiaryId: string): void {
    this.cardLoading.set(true);
    this.cardError.set(false);
    this.beneficiaryApi.getBeneficiary(beneficiaryId).subscribe({
      next: (response) => {
        this.summary.set(response);
        this.cardLoading.set(false);
        // SPEC-0006 BR3: blob-fetch the avatar only when the backend reports one (avoids a 404).
        if (response.avatarUrl) {
          this.avatar.load(beneficiaryId);
        }
      },
      error: () => {
        this.cardError.set(true);
        this.cardLoading.set(false);
      },
    });
  }

  retryCard(): void {
    const activeId = this.context.active()?.beneficiaryId;
    if (activeId) {
      this.loadSummary(activeId);
    }
  }

  loadContent(): void {
    this.homeApi.getHomeContent().subscribe({
      next: (response) => {
        this.banners.set(response.banners);
        this.notices.set(response.notices);
        const [firstNotice] = this.sortedNotices();
        this.openNoticeValue.set(firstNotice ? firstNotice.order : null);
        this.startBannerRotation();
      },
      // BR8: content unavailable must not break the Home — sections simply stay hidden (empty
      // arrays keep the `@if (...length > 0)` guards closed); card and shortcuts keep rendering.
      error: () => {
        console.error(
          'home.content.load-failed — GET /api/content/home unavailable; banners/notices hidden (BR8).',
        );
      },
    });
  }

  initialOf(name: string | undefined): string {
    return name ? name.charAt(0).toUpperCase() : '?';
  }

  onAvatarClick(): void {
    this.dialogMessageKey.set('home.cartao.avatarEmBreve');
  }

  onShortcutClick(shortcut: QuickAccessShortcut): void {
    if (shortcut.key === 'reconhecimentoFacial') {
      this.dialogMessageKey.set('home.reconhecimentoFacial.mensagem');
    }
  }

  closeDialog(): void {
    this.dialogMessageKey.set(null);
  }

  onDialogVisibleChange(visible: boolean): void {
    if (!visible) {
      this.closeDialog();
    }
  }

  onNoticeValueChange(value: string | number | string[] | number[] | null | undefined): void {
    this.openNoticeValue.set(typeof value === 'number' ? value : null);
  }

  onBannerPageChange(event: { page?: number }): void {
    if (typeof event.page === 'number') {
      this.bannerPage.set(event.page);
    }
  }

  pauseBannerRotation(): void {
    this.bannerPaused = true;
  }

  resumeBannerRotation(): void {
    this.bannerPaused = false;
  }

  private startBannerRotation(): void {
    this.stopBannerRotation();
    if (this.sortedBanners().length <= 1) {
      return;
    }
    this.bannerTimer = setInterval(() => {
      if (this.bannerPaused) {
        return;
      }
      const total = this.sortedBanners().length;
      this.bannerPage.set((this.bannerPage() + 1) % total);
    }, BANNER_ROTATION_MS);
  }

  private stopBannerRotation(): void {
    if (this.bannerTimer) {
      clearInterval(this.bannerTimer);
      this.bannerTimer = undefined;
    }
  }
}
