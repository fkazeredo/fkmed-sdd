import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SelectableOption, SearchableOptionList } from '../../shared/components/searchable-option-list';
import { CONSULTORIOS_SERVICE_TYPE_CODE, NetworkApi, RegistryOption } from './network.api';
import { NetworkFunnelState } from './network-funnel-state.service';

/**
 * Specialty step (SPEC-0008 BR6): only reached when the chosen service type is
 * CONSULTORIOS–Clínicas–Terapias (BR5) — every other type skips straight to Results. Reuses the
 * shared searchable/alphabetical list widget (BR2's real-time filtering rule extends here).
 */
@Component({
  selector: 'app-network-specialty',
  imports: [TranslatePipe, SearchableOptionList],
  templateUrl: './network-specialty.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NetworkSpecialty implements OnInit {
  private readonly api = inject(NetworkApi);
  private readonly router = inject(Router);
  protected readonly funnel = inject(NetworkFunnelState);

  protected readonly specialties = signal<RegistryOption[]>([]);
  protected readonly specialtyOptions = computed<SelectableOption[]>(() =>
    this.specialties().map((option) => ({ value: option.code, label: option.name })),
  );

  protected readonly summary = computed(() => {
    const s = this.funnel.selection();
    const parts = [s.neighborhood, s.municipality].filter((part): part is string => !!part);
    return `${parts.join(', ')} – ${s.uf} · ${s.serviceTypeName}`;
  });

  ngOnInit(): void {
    if (this.funnel.selection().serviceType !== CONSULTORIOS_SERVICE_TYPE_CODE) {
      void this.router.navigate(['/rede/busca/tipo-servico']);
      return;
    }
    this.api.getSpecialties().subscribe((response) => this.specialties.set(response.items));
  }

  editServiceType(): void {
    void this.router.navigate(['/rede/busca/tipo-servico']);
  }

  select(code: string): void {
    const found = this.specialties().find((option) => option.code === code);
    if (!found) {
      return;
    }
    this.funnel.setSpecialty(found.code, found.name);
    void this.router.navigate(['/rede/busca/resultados']);
  }
}
