import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { NetworkApi, ServiceTypeOption } from './network.api';
import { NetworkFunnelState } from './network-funnel-state.service';

/**
 * "O que deseja buscar?" — service-type step (SPEC-0008 BR5): the locality summary chosen in the
 * previous screen sits on top, tappable to edit while preserving every value (BR11), followed by
 * the registry list of service types. The backend flags each type with `hasSpecialtyStep`; only
 * those go to the specialty step next — every other type skips straight to Results (AC3).
 */
@Component({
  selector: 'app-network-service-type',
  imports: [TranslatePipe],
  templateUrl: './network-service-type.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NetworkServiceType implements OnInit {
  private readonly api = inject(NetworkApi);
  private readonly router = inject(Router);
  protected readonly funnel = inject(NetworkFunnelState);

  protected readonly serviceTypes = signal<ServiceTypeOption[]>([]);

  protected readonly localitySummary = computed(() => {
    const s = this.funnel.selection();
    const parts = [s.neighborhood, s.municipality].filter((part): part is string => !!part);
    return `${parts.join(', ')} – ${s.uf}`;
  });

  ngOnInit(): void {
    if (!this.funnel.canSearchLocality()) {
      void this.router.navigate(['/rede/busca']);
      return;
    }
    this.api.getServiceTypes().subscribe((types) => this.serviceTypes.set(types));
  }

  editLocality(): void {
    void this.router.navigate(['/rede/busca']);
  }

  select(option: ServiceTypeOption): void {
    this.funnel.setServiceType(option.code, option.name, option.hasSpecialtyStep);
    if (option.hasSpecialtyStep) {
      void this.router.navigate(['/rede/busca/especialidade']);
    } else {
      void this.router.navigate(['/rede/busca/resultados']);
    }
  }
}
