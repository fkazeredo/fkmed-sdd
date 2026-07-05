import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { CONSULTORIOS_SERVICE_TYPE_CODE, NetworkApi, ProviderSearchResult } from './network.api';
import { NetworkFunnelState } from './network-funnel-state.service';
import { NetworkResults } from './network-results';

const FUNNEL_RESULT: ProviderSearchResult = {
  referenceDate: '2026-07-04',
  items: [
    {
      id: 'p1',
      name: 'Dr. João Cardiologista',
      neighborhood: 'Centro',
      municipality: 'Rio de Janeiro',
      uf: 'RJ',
      serviceType: 'Consultórios–Clínicas–Terapias',
      seals: [{ code: 'QUALI', name: 'Selo Qualidade', description: 'Boa avaliação' }],
    },
  ],
};

const NAME_RESULT: ProviderSearchResult = {
  referenceDate: '2026-07-04',
  items: [
    {
      id: 'p2',
      name: 'Clínica Cardio Rio',
      neighborhood: 'Copacabana',
      municipality: 'Rio de Janeiro',
      uf: 'RJ',
      serviceType: 'Consultórios–Clínicas–Terapias',
      seals: [],
    },
  ],
};

describe('NetworkResults', () => {
  let fixture: ComponentFixture<NetworkResults>;
  let api: { getProviders: ReturnType<typeof vi.fn>; searchProvidersByName: ReturnType<typeof vi.fn> };
  let funnel: NetworkFunnelState;
  let router: Router;
  const routeStub: { snapshot: { queryParamMap: ReturnType<typeof convertToParamMap> } } = {
    snapshot: { queryParamMap: convertToParamMap({}) },
  };

  function setup(queryParams: Record<string, string>): void {
    routeStub.snapshot.queryParamMap = convertToParamMap(queryParams);
    fixture = TestBed.createComponent(NetworkResults);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    sessionStorage.clear();
    api = {
      getProviders: vi.fn().mockReturnValue(of(FUNNEL_RESULT)),
      searchProvidersByName: vi.fn().mockReturnValue(of(NAME_RESULT)),
    };
    await TestBed.configureTestingModule({
      imports: [NetworkResults],
      providers: [
        provideI18n(),
        { provide: NetworkApi, useValue: api },
        { provide: ActivatedRoute, useValue: routeStub },
      ],
    }).compileComponents();
    funnel = TestBed.inject(NetworkFunnelState);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  describe('funnel mode (BR7)', () => {
    beforeEach(() => {
      funnel.setUf('RJ', 'Rio de Janeiro');
      funnel.setMunicipality('Rio de Janeiro');
      funnel.setNeighborhood('Centro');
      funnel.setServiceType(CONSULTORIOS_SERVICE_TYPE_CODE, 'Consultórios–Clínicas–Terapias');
      funnel.setSpecialty('CARDIOLOGIA', 'Cardiologia');
    });

    it('calls getProviders with the persisted filters', () => {
      setup({});
      expect(api.getProviders).toHaveBeenCalledWith({
        uf: 'RJ',
        municipality: 'Rio de Janeiro',
        neighborhood: 'Centro',
        serviceType: CONSULTORIOS_SERVICE_TYPE_CODE,
        specialty: 'CARDIOLOGIA',
      });
    });

    it('omits neighborhood when "Todos" (BR9) and specialty when not applicable', () => {
      funnel.setNeighborhood(null);
      funnel.setServiceType('LABORATORIOS', 'Laboratórios e Exames');
      setup({});
      expect(api.getProviders).toHaveBeenCalledWith({
        uf: 'RJ',
        municipality: 'Rio de Janeiro',
        neighborhood: undefined,
        serviceType: 'LABORATORIOS',
        specialty: undefined,
      });
    });

    it('shows the reference date and the card (name, locality, seals — no service type) (AC2)', () => {
      setup({});
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="resultados-data-referencia"]')?.textContent).toContain('2026-07-04');
      const card = el.querySelector('[data-testid="resultado-card-p1"]') as HTMLElement;
      expect(card.textContent).toContain('Dr. João Cardiologista');
      expect(card.textContent).toContain('Centro, Rio de Janeiro – RJ');
      expect(card.querySelector('[data-testid="resultado-selo-QUALI"]')).not.toBeNull();
      expect(card.querySelector('[data-testid="resultado-servico"]')).toBeNull();
    });

    it('empty result shows the BR10 message with "Alterar localidade" and "Alterar especialidade"', () => {
      api.getProviders.mockReturnValue(of({ referenceDate: '2026-07-04', items: [] }));
      setup({});
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="resultados-vazio"]')?.textContent).toContain(
        'Não encontramos prestadores para esta busca',
      );
      (el.querySelector('[data-testid="resultados-alterar-localidade"]') as HTMLElement).click();
      expect(router.navigate).toHaveBeenCalledWith(['/rede/busca']);
      expect(el.querySelector('[data-testid="resultados-alterar-especialidade"]')).not.toBeNull();
    });

    it('empty result hides "Alterar especialidade" when the service type has no specialty step', () => {
      funnel.setServiceType('LABORATORIOS', 'Laboratórios e Exames');
      api.getProviders.mockReturnValue(of({ referenceDate: '2026-07-04', items: [] }));
      setup({});
      expect(fixture.nativeElement.querySelector('[data-testid="resultados-alterar-especialidade"]')).toBeNull();
    });

    it('"Alterar especialidade" navigates to the specialty step', () => {
      api.getProviders.mockReturnValue(of({ referenceDate: '2026-07-04', items: [] }));
      setup({});
      (
        fixture.nativeElement.querySelector('[data-testid="resultados-alterar-especialidade"]') as HTMLElement
      ).click();
      expect(router.navigate).toHaveBeenCalledWith(['/rede/busca/especialidade']);
    });

    it('clicking a card navigates to the provider detail', () => {
      setup({});
      (fixture.nativeElement.querySelector('[data-testid="resultado-card-p1"]') as HTMLElement).click();
      expect(router.navigate).toHaveBeenCalledWith(['/rede/busca/prestador', 'p1']);
    });
  });

  describe('name-search mode (BR8)', () => {
    it('calls searchProvidersByName with the name and optional municipality query params', () => {
      setup({ nome: 'cardio', municipio: 'Rio de Janeiro' });
      expect(api.searchProvidersByName).toHaveBeenCalledWith('cardio', 'Rio de Janeiro');
    });

    it('omits municipality when not given in the query params', () => {
      setup({ nome: 'cardio' });
      expect(api.searchProvidersByName).toHaveBeenCalledWith('cardio', undefined);
    });

    it('shows the card with the service type too (BR8, AC8)', () => {
      setup({ nome: 'cardio' });
      const card = fixture.nativeElement.querySelector('[data-testid="resultado-card-p2"]') as HTMLElement;
      expect(card.textContent).toContain('Clínica Cardio Rio');
      expect(card.textContent).toContain('Copacabana, Rio de Janeiro – RJ');
      expect(card.querySelector('[data-testid="resultado-servico"]')?.textContent).toContain(
        'Consultórios–Clínicas–Terapias',
      );
    });

    it('empty result shows the BR10 message without the funnel-only actions', () => {
      api.searchProvidersByName.mockReturnValue(of({ referenceDate: '2026-07-04', items: [] }));
      setup({ nome: 'zzz' });
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="resultados-vazio"]')?.textContent).toContain(
        'Não encontramos prestadores para esta busca',
      );
      expect(el.querySelector('[data-testid="resultados-alterar-localidade"]')).toBeNull();
      expect(el.querySelector('[data-testid="resultados-alterar-especialidade"]')).toBeNull();
    });

    it('maps a 422 network.query-too-short to an inline message', () => {
      api.searchProvidersByName.mockReturnValue(
        throwError(() => new HttpErrorResponse({ error: { code: 'network.query-too-short' }, status: 422 })),
      );
      setup({ nome: 'ca' });
      expect(fixture.nativeElement.textContent).toContain(
        'Digite ao menos 3 caracteres para buscar por nome.',
      );
    });
  });

  it('"Pesquisar por localidade" always navigates back to the assistant (AC4)', () => {
    setup({ nome: 'cardio' });
    (fixture.nativeElement.querySelector('[data-testid="resultados-pesquisar-localidade"]') as HTMLElement).click();
    expect(router.navigate).toHaveBeenCalledWith(['/rede/busca']);
  });
});
