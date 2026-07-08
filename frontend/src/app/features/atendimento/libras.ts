import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { LibrasRequestResult, SupportApi } from './support.api';

/**
 * The Central de Libras screen (SPEC-0014 BR4): explanation + operating hours + "Solicitar
 * atendimento em Libras", registering the request for the active beneficiary and confirming
 * either an imminent videocall (within hours) or the next operating period (outside hours, AC4).
 */
@Component({
  selector: 'app-libras',
  imports: [TranslatePipe],
  templateUrl: './libras.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Libras {
  private readonly api = inject(SupportApi);
  private readonly context = inject(BeneficiaryContextService);

  protected readonly loading = signal(false);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly result = signal<LibrasRequestResult | null>(null);

  request(): void {
    const beneficiaryId = this.context.active()?.beneficiaryId;
    if (!beneficiaryId) {
      return;
    }
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.requestLibras(beneficiaryId).subscribe({
      next: (result) => {
        this.result.set(result);
        this.loading.set(false);
      },
      error: () => {
        this.errorKey.set('common.error');
        this.loading.set(false);
      },
    });
  }
}
