import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { NetworkApi } from './network.api';
import { EMPTY_FUNNEL_SELECTION, NetworkFunnelState } from './network-funnel-state.service';
import { NetworkSearch } from './network-search';

/**
 * SPEC-0008 BR1/BR2 (locality funnel: State → Municipality → Neighborhood, enable/clear rules,
 * real-time accent/case-insensitive filtering) and BR8 (name search, ≥ 3 chars, optional
 * municipality filter) — both live on the same "O que deseja buscar" entry screen.
 */
describe('NetworkSearch', () => {
  let fixture: ComponentFixture<NetworkSearch>;
  let api: {
    getStates: ReturnType<typeof vi.fn>;
    getMunicipalities: ReturnType<typeof vi.fn>;
    getNeighborhoods: ReturnType<typeof vi.fn>;
  };
  let funnel: NetworkFunnelState;
  let router: Router;

  beforeEach(async () => {
    sessionStorage.clear();
    // Real backend shape: raw arrays, no `{items:[…]}` envelope. `states` is UF codes only.
    api = {
      getStates: vi.fn().mockReturnValue(of(['RJ'])),
      getMunicipalities: vi.fn().mockReturnValue(of(['Rio de Janeiro', 'Rio Bonito', 'Niterói', 'Cabo Frio'])),
      getNeighborhoods: vi.fn().mockReturnValue(of(['Centro', 'Copacabana', 'Tijuca'])),
    };
    await TestBed.configureTestingModule({
      imports: [NetworkSearch],
      providers: [provideI18n(), { provide: NetworkApi, useValue: api }],
    }).compileComponents();
    fixture = TestBed.createComponent(NetworkSearch);
    funnel = TestBed.inject(NetworkFunnelState);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  function openDialog(testid: string): void {
    (fixture.nativeElement.querySelector(`[data-testid="${testid}"]`) as HTMLElement).click();
    fixture.detectChanges();
  }

  describe('locality funnel (BR1)', () => {
    it('starts with Municipality and Neighborhood disabled, and Buscar disabled', () => {
      const el = fixture.nativeElement as HTMLElement;
      expect((el.querySelector('[data-testid="funil-municipio"]') as HTMLButtonElement).disabled).toBe(true);
      expect((el.querySelector('[data-testid="funil-bairro"]') as HTMLButtonElement).disabled).toBe(true);
      expect((el.querySelector('[data-testid="funil-buscar"]') as HTMLButtonElement).disabled).toBe(true);
    });

    it('choosing a State enables Municipality (BR1)', () => {
      openDialog('funil-uf');
      openDialog('option-item-RJ');

      const el = fixture.nativeElement as HTMLElement;
      // The backend sends only UF codes, so the funnel shows the code ("RJ") as the state label.
      expect(el.querySelector('[data-testid="funil-uf"]')?.textContent).toContain('RJ');
      expect((el.querySelector('[data-testid="funil-municipio"]') as HTMLButtonElement).disabled).toBe(false);
      expect(funnel.selection().uf).toBe('RJ');
    });

    it('choosing a Municipality enables Neighborhood and "Buscar" (BR1)', () => {
      openDialog('funil-uf');
      openDialog('option-item-RJ');
      openDialog('funil-municipio');
      openDialog('option-item-Rio de Janeiro');

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="funil-municipio"]')?.textContent).toContain('Rio de Janeiro');
      expect((el.querySelector('[data-testid="funil-bairro"]') as HTMLButtonElement).disabled).toBe(false);
      expect((el.querySelector('[data-testid="funil-buscar"]') as HTMLButtonElement).disabled).toBe(false);
    });

    it('"Buscar" stays enabled without a Neighborhood — optional, defaults to "Todos" (BR1)', () => {
      openDialog('funil-uf');
      openDialog('option-item-RJ');
      openDialog('funil-municipio');
      openDialog('option-item-Rio de Janeiro');

      expect(funnel.selection().neighborhood).toBeNull();
      expect((fixture.nativeElement.querySelector('[data-testid="funil-buscar"]') as HTMLButtonElement).disabled).toBe(
        false,
      );
    });

    it('choosing "Todos" in the neighborhood dialog keeps neighborhood null (BR9)', () => {
      openDialog('funil-uf');
      openDialog('option-item-RJ');
      openDialog('funil-municipio');
      openDialog('option-item-Rio de Janeiro');
      openDialog('funil-bairro');
      openDialog('funil-bairro-todos');

      expect(funnel.selection().neighborhood).toBeNull();
      expect(fixture.nativeElement.querySelector('[data-testid="funil-bairro"]')?.textContent).toContain('Todos');
    });

    it('choosing a Neighborhood sets it (BR1)', () => {
      openDialog('funil-uf');
      openDialog('option-item-RJ');
      openDialog('funil-municipio');
      openDialog('option-item-Rio de Janeiro');
      openDialog('funil-bairro');
      openDialog('option-item-Centro');

      expect(funnel.selection().neighborhood).toBe('Centro');
      expect(fixture.nativeElement.querySelector('[data-testid="funil-bairro"]')?.textContent).toContain('Centro');
    });

    it('changing the State clears Municipality and Neighborhood (BR1, AC7)', () => {
      openDialog('funil-uf');
      openDialog('option-item-RJ');
      openDialog('funil-municipio');
      openDialog('option-item-Rio de Janeiro');
      openDialog('funil-bairro');
      openDialog('option-item-Centro');

      // Re-opens the State dialog and picks the same option again — the clearing rule must still
      // fire, since BR1 doesn't carve out "picking the same value" as an exception.
      openDialog('funil-uf');
      openDialog('option-item-RJ');

      expect(funnel.selection().municipality).toBeNull();
      expect(funnel.selection().neighborhood).toBeNull();
      const el = fixture.nativeElement as HTMLElement;
      // Municipality re-enables immediately (uf is still set) but its VALUE is cleared back to
      // the placeholder; Neighborhood stays disabled again since no municipality is chosen yet.
      expect(el.querySelector('[data-testid="funil-municipio"]')?.textContent).toContain('Selecionar');
      expect((el.querySelector('[data-testid="funil-bairro"]') as HTMLButtonElement).disabled).toBe(true);
    });

    it('filters municipalities in real time as the user types (BR2, AC1) and re-fetches from the server', () => {
      openDialog('funil-uf');
      openDialog('option-item-RJ');
      openDialog('funil-municipio');

      expect(api.getMunicipalities).toHaveBeenCalledWith('RJ', undefined);

      const input = fixture.nativeElement.querySelector(
        '[data-testid="funil-municipio-dialog"] [data-testid="option-search-input"]',
      ) as HTMLInputElement;
      input.value = 'rio';
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(api.getMunicipalities).toHaveBeenCalledWith('RJ', 'rio');
    });

    it('navigates to /rede/busca/tipo-servico on "Buscar" (BR1)', () => {
      openDialog('funil-uf');
      openDialog('option-item-RJ');
      openDialog('funil-municipio');
      openDialog('option-item-Rio de Janeiro');

      (fixture.nativeElement.querySelector('[data-testid="funil-buscar"]') as HTMLElement).click();
      expect(router.navigate).toHaveBeenCalledWith(['/rede/busca/tipo-servico']);
    });

    it('restores a previously persisted selection (BR11)', async () => {
      funnel.setUf('RJ', 'Rio de Janeiro');
      funnel.setMunicipality('Rio de Janeiro');
      funnel.setNeighborhood('Centro');

      const restoredFixture = TestBed.createComponent(NetworkSearch);
      restoredFixture.detectChanges();
      const el = restoredFixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="funil-uf"]')?.textContent).toContain('Rio de Janeiro');
      expect(el.querySelector('[data-testid="funil-municipio"]')?.textContent).toContain('Rio de Janeiro');
      expect(el.querySelector('[data-testid="funil-bairro"]')?.textContent).toContain('Centro');
    });
  });

  describe('name search (BR8)', () => {
    function typeName(value: string): void {
      const input = fixture.nativeElement.querySelector('[data-testid="nome-input"]') as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();
    }

    it('disables "Buscar" below 3 characters', () => {
      typeName('ca');
      expect((fixture.nativeElement.querySelector('[data-testid="nome-buscar"]') as HTMLButtonElement).disabled).toBe(
        true,
      );
    });

    it('enables "Buscar" at 3+ characters and navigates to results with the name query (AC8)', () => {
      typeName('cardio');
      const button = fixture.nativeElement.querySelector('[data-testid="nome-buscar"]') as HTMLButtonElement;
      expect(button.disabled).toBe(false);

      button.click();
      expect(router.navigate).toHaveBeenCalledWith(['/rede/busca/resultados'], {
        queryParams: { nome: 'cardio' },
      });
    });

    it('ignores leading/trailing whitespace when counting the 3-char minimum', () => {
      typeName('  ca  ');
      expect((fixture.nativeElement.querySelector('[data-testid="nome-buscar"]') as HTMLButtonElement).disabled).toBe(
        true,
      );
      typeName('  car  ');
      expect((fixture.nativeElement.querySelector('[data-testid="nome-buscar"]') as HTMLButtonElement).disabled).toBe(
        false,
      );
    });
  });

  it('leaves the funnel selection untouched on load (no forced clear on mount)', () => {
    expect(funnel.selection()).toEqual(EMPTY_FUNNEL_SELECTION);
  });
});
