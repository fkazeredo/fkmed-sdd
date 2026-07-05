import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { NetworkApi, ProviderDetail } from './network.api';

const COPY_CONFIRMATION_MS = 3000;

/**
 * Provider detail (SPEC-0008 BR12): name, service type, specialties, full address, clickable
 * phone, seals (name always visible, description on hover/touch — implemented as a click-to-toggle
 * so it works the same for mouse and touch), "Traçar rota" (opens the full address in the external
 * maps service, new tab — out of scope: an embedded map) and "Copiar endereço" (mirrors the
 * Carteirinha "Copiar número" confirmation pattern, SPEC-0007). BR13: an inactive/unknown provider
 * answers `410 network.provider-unavailable` — rendered as a dedicated state, not a generic error.
 */
@Component({
  selector: 'app-network-provider-detail',
  imports: [TranslatePipe],
  templateUrl: './network-provider-detail.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NetworkProviderDetail implements OnInit, OnDestroy {
  private readonly api = inject(NetworkApi);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(true);
  readonly unavailable = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly provider = signal<ProviderDetail | null>(null);

  readonly openSealCode = signal<string | null>(null);
  readonly addressCopied = signal(false);
  private copyTimer: ReturnType<typeof setTimeout> | undefined;

  readonly fullAddress = computed(() => {
    const p = this.provider();
    if (!p) {
      return '';
    }
    const { street, number, complement, neighborhood, municipality, uf, cep } = p.address;
    const line1 = complement ? `${street}, ${number} - ${complement}` : `${street}, ${number}`;
    return `${line1}, ${neighborhood}, ${municipality} - ${uf}, ${cep}`;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.api.getProvider(id).subscribe({
      next: (detail) => {
        this.provider.set(detail);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        if (error.error?.code === 'network.provider-unavailable') {
          this.unavailable.set(true);
        } else {
          this.errorKey.set('common.error');
        }
      },
    });
  }

  ngOnDestroy(): void {
    if (this.copyTimer) {
      clearTimeout(this.copyTimer);
    }
  }

  toggleSeal(code: string): void {
    this.openSealCode.update((current) => (current === code ? null : code));
  }

  tracarRota(): void {
    const url = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(this.fullAddress())}`;
    window.open(url, '_blank');
  }

  copiarEndereco(): void {
    void navigator.clipboard.writeText(this.fullAddress()).then(() => {
      this.addressCopied.set(true);
      if (this.copyTimer) {
        clearTimeout(this.copyTimer);
      }
      this.copyTimer = setTimeout(() => this.addressCopied.set(false), COPY_CONFIRMATION_MS);
    });
  }
}
