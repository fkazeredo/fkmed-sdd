import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { CONSULTORIOS_SERVICE_TYPE_CODE, NetworkApi } from './network.api';
import { NetworkFunnelState } from './network-funnel-state.service';
import { NetworkServiceType } from './network-service-type';

const SERVICE_TYPES = [
  { code: CONSULTORIOS_SERVICE_TYPE_CODE, name: 'Consultórios–Clínicas–Terapias' },
  { code: 'LABORATORIOS', name: 'Laboratórios e Exames' },
  { code: 'HEMODIALISE', name: 'Hemodiálise' },
];

/**
 * SPEC-0008 BR5 ("O que deseja buscar?": service-type list, only CONSULTORIOS has a specialty
 * step) and BR11 (the locality summary on top is tappable to edit, preserving the values).
 */
describe('NetworkServiceType', () => {
  let fixture: ComponentFixture<NetworkServiceType>;
  let api: { getServiceTypes: ReturnType<typeof vi.fn> };
  let funnel: NetworkFunnelState;
  let router: Router;

  beforeEach(async () => {
    sessionStorage.clear();
    api = { getServiceTypes: vi.fn().mockReturnValue(of({ items: SERVICE_TYPES })) };
    await TestBed.configureTestingModule({
      imports: [NetworkServiceType],
      providers: [provideI18n(), { provide: NetworkApi, useValue: api }],
    }).compileComponents();
    funnel = TestBed.inject(NetworkFunnelState);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  function setup(): void {
    funnel.setUf('RJ', 'Rio de Janeiro');
    funnel.setMunicipality('Rio de Janeiro');
    funnel.setNeighborhood('Centro');
    fixture = TestBed.createComponent(NetworkServiceType);
    fixture.detectChanges();
  }

  it('redirects to /rede/busca when the locality has not been chosen (defensive)', () => {
    fixture = TestBed.createComponent(NetworkServiceType);
    fixture.detectChanges();
    expect(router.navigate).toHaveBeenCalledWith(['/rede/busca']);
  });

  it('shows the locality summary "BAIRRO, MUNICÍPIO – UF" (BR11)', () => {
    setup();
    expect(fixture.nativeElement.querySelector('[data-testid="tipo-servico-localidade-resumo"]')?.textContent).toContain(
      'Centro, Rio de Janeiro – RJ',
    );
  });

  it('shows "Todos" in the summary when no neighborhood was chosen (BR9)', () => {
    funnel.setUf('RJ', 'Rio de Janeiro');
    funnel.setMunicipality('Rio de Janeiro');
    fixture = TestBed.createComponent(NetworkServiceType);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="tipo-servico-localidade-resumo"]')?.textContent).toContain(
      'Rio de Janeiro – RJ',
    );
  });

  it('tapping the locality summary returns to /rede/busca, preserving the selection (BR11, AC4)', () => {
    setup();
    (fixture.nativeElement.querySelector('[data-testid="tipo-servico-localidade-resumo"]') as HTMLElement).click();
    expect(router.navigate).toHaveBeenCalledWith(['/rede/busca']);
    // BR11: navigating back does not clear the persisted selection.
    expect(funnel.selection().uf).toBe('RJ');
    expect(funnel.selection().neighborhood).toBe('Centro');
  });

  it('lists every service type from the registry', () => {
    setup();
    for (const type of SERVICE_TYPES) {
      expect(fixture.nativeElement.querySelector(`[data-testid="tipo-servico-item-${type.code}"]`)?.textContent).toContain(
        type.name,
      );
    }
  });

  it('choosing CONSULTORIOS navigates to the specialty step (BR5)', () => {
    setup();
    (
      fixture.nativeElement.querySelector(
        `[data-testid="tipo-servico-item-${CONSULTORIOS_SERVICE_TYPE_CODE}"]`,
      ) as HTMLElement
    ).click();
    expect(funnel.selection().serviceType).toBe(CONSULTORIOS_SERVICE_TYPE_CODE);
    expect(router.navigate).toHaveBeenCalledWith(['/rede/busca/especialidade']);
  });

  it('choosing any other service type skips straight to results (BR5, AC3)', () => {
    setup();
    (fixture.nativeElement.querySelector('[data-testid="tipo-servico-item-LABORATORIOS"]') as HTMLElement).click();
    expect(funnel.selection().serviceType).toBe('LABORATORIOS');
    expect(router.navigate).toHaveBeenCalledWith(['/rede/busca/resultados']);
  });
});
