import { computed, Injectable, signal } from '@angular/core';
import { CONSULTORIOS_SERVICE_TYPE_CODE } from './network.api';

/** sessionStorage key for the funnel's in-progress selections — tab-scoped (mirrors
 * `core/context/beneficiary-context.service.ts`'s `ACTIVE_BENEFICIARY_KEY` rationale). */
export const NETWORK_FUNNEL_KEY = 'fkmed.networkFunnel';

export interface FunnelSelection {
  uf: string | null;
  ufName: string | null;
  municipality: string | null;
  /** `null` means "Todos" — not chosen, or explicitly cleared (BR9). */
  neighborhood: string | null;
  serviceType: string | null;
  serviceTypeName: string | null;
  specialty: string | null;
  specialtyName: string | null;
}

export const EMPTY_FUNNEL_SELECTION: FunnelSelection = {
  uf: null,
  ufName: null,
  municipality: null,
  neighborhood: null,
  serviceType: null,
  serviceTypeName: null,
  specialty: null,
  specialtyName: null,
};

/**
 * State of the "Assistente de funil" (SPEC-0008 BR1/BR11): State → Municipality → Neighborhood →
 * service type → specialty. Persisted to `sessionStorage` so navigating to Results and back (AC4,
 * "Pesquisar por localidade") — which destroys and rebuilds the wizard's routed components —
 * re-presents exactly what was chosen. BR1's clearing rules live here, not in the components, so
 * every screen that mutates a step sees the same guarantees.
 */
@Injectable({ providedIn: 'root' })
export class NetworkFunnelState {
  readonly selection = signal<FunnelSelection>(this.restore());

  /** BR1: "Buscar" enables with State + Municipality; Neighborhood is optional ("Todos"). */
  readonly canSearchLocality = computed(() => {
    const s = this.selection();
    return !!s.uf && !!s.municipality;
  });

  /** BR5: only "Consultórios–Clínicas–Terapias" has the specialty step. */
  readonly hasSpecialtyStep = computed(() => this.selection().serviceType === CONSULTORIOS_SERVICE_TYPE_CODE);

  /** BR1: changing State clears Municipality and Neighborhood. */
  setUf(code: string, name: string): void {
    this.update({ ...this.selection(), uf: code, ufName: name, municipality: null, neighborhood: null });
  }

  /** BR1: changing Municipality clears Neighborhood. */
  setMunicipality(name: string): void {
    this.update({ ...this.selection(), municipality: name, neighborhood: null });
  }

  /** BR9: `null` represents "Todos" (the whole municipality). */
  setNeighborhood(name: string | null): void {
    this.update({ ...this.selection(), neighborhood: name });
  }

  /** Changing the service type invalidates any previously chosen specialty (it only ever applied
   * to CONSULTORIOS, BR5). */
  setServiceType(code: string, name: string): void {
    this.update({
      ...this.selection(),
      serviceType: code,
      serviceTypeName: name,
      specialty: null,
      specialtyName: null,
    });
  }

  setSpecialty(code: string, name: string): void {
    this.update({ ...this.selection(), specialty: code, specialtyName: name });
  }

  clear(): void {
    this.update(EMPTY_FUNNEL_SELECTION);
  }

  private update(next: FunnelSelection): void {
    this.selection.set(next);
    sessionStorage.setItem(NETWORK_FUNNEL_KEY, JSON.stringify(next));
  }

  private restore(): FunnelSelection {
    try {
      const raw = sessionStorage.getItem(NETWORK_FUNNEL_KEY);
      if (!raw) {
        return { ...EMPTY_FUNNEL_SELECTION };
      }
      return { ...EMPTY_FUNNEL_SELECTION, ...(JSON.parse(raw) as Partial<FunnelSelection>) };
    } catch {
      return { ...EMPTY_FUNNEL_SELECTION };
    }
  }
}
