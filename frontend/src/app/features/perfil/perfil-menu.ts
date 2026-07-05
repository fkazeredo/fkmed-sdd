import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AccordionModule } from 'primeng/accordion';
import { DialogModule } from 'primeng/dialog';
import { AuthService } from '../../core/auth/auth.service';
import { APP_VERSION } from '../../core/config/app-version';
import { AvatarStateService } from '../../core/context/avatar-state.service';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import {
  BeneficiarySummary,
  BeneficiarySummaryApi,
} from '../../core/context/beneficiary-summary.api';

/**
 * The Perfil menu (SPEC-0006 BR1): the header card ("Olá, {NOME}" + plan + card number, keyed off
 * the active beneficiary — SPEC-0003) followed by the fixed-order items (Alterar Foto, Segurança,
 * Alterar Cadastro, Central de Libras / Perguntas Frequentes both "em breve" → SPEC-0014,
 * Comunicado de privacidade, Termos de uso, Sair) with the build version right-aligned by Sair
 * (BR10), and the expandable LGPD notice (same PrimeNG accordion the Home notices use). Sair asks
 * for confirmation before ending the session (BR9). The avatar reflects the shared avatar state so
 * a photo change elsewhere shows here without a new login (BR3).
 */
@Component({
  selector: 'app-perfil-menu',
  imports: [RouterLink, TranslatePipe, AccordionModule, DialogModule],
  templateUrl: './perfil-menu.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PerfilMenu {
  private readonly summaryApi = inject(BeneficiarySummaryApi);
  protected readonly context = inject(BeneficiaryContextService);
  protected readonly avatar = inject(AvatarStateService);
  private readonly auth = inject(AuthService);
  protected readonly version = inject(APP_VERSION);

  readonly summary = signal<BeneficiarySummary | null>(null);
  readonly confirmingLogout = signal(false);

  /** The avatar (blob object URL) for the active beneficiary; null → placeholder (BR3). */
  readonly avatarUrl = computed(() => this.avatar.avatarUrl(this.context.active()?.beneficiaryId));

  constructor() {
    effect(() => {
      const id = this.context.active()?.beneficiaryId;
      if (id) {
        this.loadSummary(id);
      }
    });
  }

  loadSummary(beneficiaryId: string): void {
    this.summaryApi.getBeneficiary(beneficiaryId).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        // Blob-fetch the photo only when the backend reports one exists (avatarUrl set) — avoids a
        // needless 404 round-trip when there is no photo.
        if (summary.avatarUrl) {
          this.avatar.load(beneficiaryId);
        }
      },
      error: () => this.summary.set(null),
    });
  }

  initialOf(name: string | undefined): string {
    return name ? name.charAt(0).toUpperCase() : '?';
  }

  askLogout(): void {
    this.confirmingLogout.set(true);
  }

  cancelLogout(): void {
    this.confirmingLogout.set(false);
  }

  confirmLogout(): void {
    this.confirmingLogout.set(false);
    this.auth.logout();
  }

  onLogoutVisibleChange(visible: boolean): void {
    if (!visible) {
      this.confirmingLogout.set(false);
    }
  }
}
