import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { CONSULTORIOS_SERVICE_TYPE_CODE, NetworkApi, RegistryOption } from './network.api';
import { NetworkFunnelState } from './network-funnel-state.service';

/**
 * "O que deseja buscar?" — service-type step (SPEC-0008 BR5): the locality summary chosen in the
 * previous screen sits on top, tappable to edit while preserving every value (BR11), followed by
 * the fixed registry list of service types. Only CONSULTORIOS–Clínicas–Terapias has a specialty
 * step next; every other type skips straight to Results (AC3).
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

  protected readonly serviceTypes = signal<RegistryOption[]>([]);

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
    this.api.getServiceTypes().subscribe((response) => this.serviceTypes.set(response.items));
  }

  editLocality(): void {
    void this.router.navigate(['/rede/busca']);
  }

  select(option: RegistryOption): void {
    this.funnel.setServiceType(option.code, option.name);
    if (option.code === CONSULTORIOS_SERVICE_TYPE_CODE) {
      void this.router.navigate(['/rede/busca/especialidade']);
    } else {
      void this.router.navigate(['/rede/busca/resultados']);
    }
  }
}
