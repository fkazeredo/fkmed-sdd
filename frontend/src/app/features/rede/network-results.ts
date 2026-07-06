import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { NetworkApi, ProviderCard, ProviderSearchResult } from './network.api';
import { NetworkFunnelState } from './network-funnel-state.service';

/**
 * Results screen (SPEC-0008 BR7/BR8/BR9/BR10): reached from either the locality funnel (reads
 * NetworkFunnelState) or the name search (reads the `nome`/`municipio` query params) — the same
 * card format either way, with the name-search cards additionally showing the service type (BR8).
 * "Pesquisar por localidade" always returns to the assistant (AC4); the empty state's
 * "Alterar localidade" / "Alterar especialidade" only make sense for the funnel (BR10).
 */
@Component({
  selector: 'app-network-results',
  imports: [TranslatePipe],
  templateUrl: './network-results.html',
  // The host must be a block box: as the default inline host it does not establish a proper
  // containing block for the centered `mx-auto` section, so at the top of a tall results page the
  // host itself becomes the hit-test target over the "Pesquisar por localidade" button (Playwright:
  // "<app-network-results> intercepts pointer events"). display:block makes the host wrap its
  // content exactly, so the button receives its own clicks.
  styles: ':host { display: block; }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NetworkResults implements OnInit {
  private readonly api = inject(NetworkApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly funnel = inject(NetworkFunnelState);

  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly referenceDate = signal<string | null>(null);
  protected readonly items = signal<ProviderCard[]>([]);

  /** Name-search mode when `nome` is present in the route's query params; otherwise funnel mode. */
  protected isNameMode = false;
  protected showServiceType = false;

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const nome = params.get('nome');
    if (nome) {
      this.isNameMode = true;
      this.showServiceType = true;
      this.load(this.api.searchProvidersByName(nome, params.get('municipio') ?? undefined));
    } else {
      const s = this.funnel.selection();
      this.load(
        this.api.getProviders({
          uf: s.uf ?? '',
          municipality: s.municipality ?? '',
          neighborhood: s.neighborhood ?? undefined,
          serviceType: s.serviceType ?? '',
          specialty: s.specialty ?? undefined,
        }),
      );
    }
  }

  private load(request: Observable<ProviderSearchResult>): void {
    this.loading.set(true);
    this.errorKey.set(null);
    request.subscribe({
      next: (response) => {
        this.referenceDate.set(response.referenceDate);
        this.items.set(response.items);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorKey.set(
          error.error?.code === 'network.query-too-short' ? 'rede.resultados.erro.buscaCurta' : 'common.error',
        );
        this.loading.set(false);
      },
    });
  }

  pesquisarPorLocalidade(): void {
    void this.router.navigate(['/rede/busca']);
  }

  alterarLocalidade(): void {
    void this.router.navigate(['/rede/busca']);
  }

  alterarEspecialidade(): void {
    void this.router.navigate(['/rede/busca/especialidade']);
  }

  abrirDetalhe(id: string): void {
    void this.router.navigate(['/rede/busca/prestador', id]);
  }
}
