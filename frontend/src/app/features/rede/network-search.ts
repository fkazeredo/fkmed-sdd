import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { DialogModule } from 'primeng/dialog';
import { SelectableOption, SearchableOptionList } from '../../shared/components/searchable-option-list';
import { NetworkFunnelState } from './network-funnel-state.service';
import { NetworkApi } from './network.api';

const NAME_MIN_LENGTH = 3;

/**
 * "O que deseja buscar" entry screen (SPEC-0008): the locality funnel (BR1 — State → Municipality
 * → Neighborhood, enable/clear rules, BR2 real-time accent/case-insensitive filtering) and the
 * name search (BR8, ≥ 3 chars, optional municipality filter) live side by side — two independent
 * ways into the same Results screen. The funnel's selections are owned by NetworkFunnelState
 * (BR11 session persistence); the name query is this screen's own transient state (BR8 has no
 * persistence requirement).
 */
@Component({
  selector: 'app-network-search',
  imports: [FormsModule, TranslatePipe, DialogModule, SearchableOptionList],
  templateUrl: './network-search.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NetworkSearch implements OnInit {
  private readonly api = inject(NetworkApi);
  private readonly router = inject(Router);
  protected readonly funnel = inject(NetworkFunnelState);

  protected readonly states = signal<string[]>([]);
  protected readonly municipalities = signal<string[]>([]);
  protected readonly neighborhoods = signal<string[]>([]);

  protected readonly ufDialogOpen = signal(false);
  protected readonly municipioDialogOpen = signal(false);
  protected readonly bairroDialogOpen = signal(false);
  protected readonly nomeMunicipioDialogOpen = signal(false);

  /** BR8's optional municipality filter only applies when the plan covers exactly one state —
   * with more than one, the filter would itself need a State step first, which BR8 does not ask
   * for; this is a documented simplification for the seeded ESTADUAL/RJ coverage (DL-0014). */
  protected readonly nomeMunicipioFiltroDisponivel = signal(false);
  protected readonly nomeMunicipioOptions = signal<string[]>([]);
  protected readonly nomeMunicipio = signal<string | null>(null);
  private nomeMunicipioUf: string | null = null;

  nameQuery = '';

  get nameSearchValid(): boolean {
    return this.nameQuery.trim().length >= NAME_MIN_LENGTH;
  }

  // `states` is a raw `string[]` of UF codes (the backend does not send a display name); for this
  // single-locale, coverage-limited product the code IS the label shown in the funnel.
  protected readonly ufOptions = () =>
    this.states().map((uf): SelectableOption => ({ value: uf, label: uf }));
  protected readonly municipalityOptions = () =>
    this.municipalities().map((name): SelectableOption => ({ value: name, label: name }));
  protected readonly neighborhoodOptions = () =>
    this.neighborhoods().map((name): SelectableOption => ({ value: name, label: name }));
  protected readonly nomeMunicipioOptionList = () =>
    this.nomeMunicipioOptions().map((name): SelectableOption => ({ value: name, label: name }));

  ngOnInit(): void {
    this.api.getStates().subscribe((states) => {
      this.states.set(states);
      if (states.length === 1) {
        this.nomeMunicipioUf = states[0];
        this.nomeMunicipioFiltroDisponivel.set(true);
      }
    });
  }

  openUfDialog(): void {
    this.ufDialogOpen.set(true);
  }

  selectUf(code: string): void {
    const found = this.states().find((uf) => uf === code);
    if (found) {
      this.funnel.setUf(found, found);
    }
    this.ufDialogOpen.set(false);
  }

  openMunicipioDialog(): void {
    const uf = this.funnel.selection().uf;
    if (!uf) {
      return;
    }
    this.fetchMunicipalities(uf, undefined);
    this.municipioDialogOpen.set(true);
  }

  onMunicipioQueryChange(query: string): void {
    const uf = this.funnel.selection().uf;
    if (!uf) {
      return;
    }
    this.fetchMunicipalities(uf, query || undefined);
  }

  private fetchMunicipalities(uf: string, query: string | undefined): void {
    this.api.getMunicipalities(uf, query).subscribe((names) => this.municipalities.set(names));
  }

  selectMunicipio(name: string): void {
    this.funnel.setMunicipality(name);
    this.municipioDialogOpen.set(false);
  }

  openBairroDialog(): void {
    const { uf, municipality } = this.funnel.selection();
    if (!uf || !municipality) {
      return;
    }
    this.api.getNeighborhoods(uf, municipality).subscribe((names) => this.neighborhoods.set(names));
    this.bairroDialogOpen.set(true);
  }

  selectBairro(name: string): void {
    this.funnel.setNeighborhood(name);
    this.bairroDialogOpen.set(false);
  }

  /** BR9: "Todos" returns providers of the whole municipality — represented as `null`. */
  selectTodosBairros(): void {
    this.funnel.setNeighborhood(null);
    this.bairroDialogOpen.set(false);
  }

  buscarFunil(): void {
    if (!this.funnel.canSearchLocality()) {
      return;
    }
    void this.router.navigate(['/rede/busca/tipo-servico']);
  }

  openNomeMunicipioDialog(): void {
    if (!this.nomeMunicipioUf) {
      return;
    }
    this.api
      .getMunicipalities(this.nomeMunicipioUf, undefined)
      .subscribe((names) => this.nomeMunicipioOptions.set(names));
    this.nomeMunicipioDialogOpen.set(true);
  }

  onNomeMunicipioQueryChange(query: string): void {
    if (!this.nomeMunicipioUf) {
      return;
    }
    this.api
      .getMunicipalities(this.nomeMunicipioUf, query || undefined)
      .subscribe((names) => this.nomeMunicipioOptions.set(names));
  }

  selectNomeMunicipio(name: string): void {
    this.nomeMunicipio.set(name);
    this.nomeMunicipioDialogOpen.set(false);
  }

  clearNomeMunicipio(): void {
    this.nomeMunicipio.set(null);
  }

  buscarNome(): void {
    if (!this.nameSearchValid) {
      return;
    }
    const queryParams: Record<string, string> = { nome: this.nameQuery.trim() };
    const municipio = this.nomeMunicipio();
    if (municipio) {
      queryParams['municipio'] = municipio;
    }
    void this.router.navigate(['/rede/busca/resultados'], { queryParams });
  }
}
