import { TestBed } from '@angular/core/testing';
import { EMPTY_FUNNEL_SELECTION, NETWORK_FUNNEL_KEY, NetworkFunnelState } from './network-funnel-state.service';

/**
 * SPEC-0008 BR1 (funnel enable/clear rules) and BR11 (session persistence across the funnel
 * screens — returning from results re-presents the chosen values).
 */
describe('NetworkFunnelState', () => {
  let service: NetworkFunnelState;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(NetworkFunnelState);
  });

  it('starts empty when nothing is persisted', () => {
    expect(service.selection()).toEqual(EMPTY_FUNNEL_SELECTION);
    expect(service.canSearchLocality()).toBe(false);
  });

  it('setUf() sets the state and clears municipality/neighborhood (BR1, AC7)', () => {
    service.setMunicipality('Rio de Janeiro');
    service.setNeighborhood('Centro');
    service.setUf('RJ', 'Rio de Janeiro');

    expect(service.selection().uf).toBe('RJ');
    expect(service.selection().ufName).toBe('Rio de Janeiro');
    expect(service.selection().municipality).toBeNull();
    expect(service.selection().neighborhood).toBeNull();
  });

  it('setMunicipality() clears neighborhood only (BR1)', () => {
    service.setUf('RJ', 'Rio de Janeiro');
    service.setMunicipality('Rio de Janeiro');
    service.setNeighborhood('Centro');

    service.setMunicipality('Niterói');

    expect(service.selection().uf).toBe('RJ');
    expect(service.selection().municipality).toBe('Niterói');
    expect(service.selection().neighborhood).toBeNull();
  });

  it('canSearchLocality is true with State + Municipality, Neighborhood optional (BR1)', () => {
    service.setUf('RJ', 'Rio de Janeiro');
    expect(service.canSearchLocality()).toBe(false);

    service.setMunicipality('Rio de Janeiro');
    expect(service.canSearchLocality()).toBe(true);
  });

  it('setServiceType() clears specialty and records the backend hasSpecialtyStep flag (BR5)', () => {
    service.setUf('RJ', 'Rio de Janeiro');
    service.setMunicipality('Rio de Janeiro');
    service.setServiceType('CONSULTORIOS', 'Consultórios–Clínicas–Terapias', true);
    service.setSpecialty('CARDIOLOGIA', 'Cardiologia');
    expect(service.hasSpecialtyStep()).toBe(true);

    // Switching to a type WITHOUT a specialty step clears the specialty and flips the flag.
    service.setServiceType('LABORATORIOS', 'Laboratórios e Exames', false);
    expect(service.selection().specialty).toBeNull();
    expect(service.selection().specialtyName).toBeNull();
    expect(service.hasSpecialtyStep()).toBe(false);
  });

  it('clear() resets to empty', () => {
    service.setUf('RJ', 'Rio de Janeiro');
    service.setMunicipality('Rio de Janeiro');
    service.clear();
    expect(service.selection()).toEqual(EMPTY_FUNNEL_SELECTION);
  });

  it('persists selections to sessionStorage and restores them on a fresh instance (BR11)', () => {
    service.setUf('RJ', 'Rio de Janeiro');
    service.setMunicipality('Rio de Janeiro');
    service.setNeighborhood('Centro');
    service.setServiceType('CONSULTORIOS', 'Consultórios–Clínicas–Terapias', true);
    service.setSpecialty('CARDIOLOGIA', 'Cardiologia');

    expect(JSON.parse(sessionStorage.getItem(NETWORK_FUNNEL_KEY) ?? '{}')).toMatchObject({
      uf: 'RJ',
      municipality: 'Rio de Janeiro',
      neighborhood: 'Centro',
    });

    // A fresh injector simulates returning from results (AC4): the wizard component was
    // destroyed and rebuilt, but the singleton restores from sessionStorage on construction.
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const restored = TestBed.inject(NetworkFunnelState);

    expect(restored.selection()).toEqual({
      uf: 'RJ',
      ufName: 'Rio de Janeiro',
      municipality: 'Rio de Janeiro',
      neighborhood: 'Centro',
      serviceType: 'CONSULTORIOS',
      serviceTypeName: 'Consultórios–Clínicas–Terapias',
      serviceTypeHasSpecialtyStep: true,
      specialty: 'CARDIOLOGIA',
      specialtyName: 'Cardiologia',
    });
    expect(restored.hasSpecialtyStep()).toBe(true);
  });

  it('setNeighborhood(null) represents "Todos" (BR9)', () => {
    service.setUf('RJ', 'Rio de Janeiro');
    service.setMunicipality('Rio de Janeiro');
    service.setNeighborhood('Centro');
    service.setNeighborhood(null);
    expect(service.selection().neighborhood).toBeNull();
  });

  it('ignores a corrupted sessionStorage value and starts empty', () => {
    sessionStorage.setItem(NETWORK_FUNNEL_KEY, '{not-json');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const restored = TestBed.inject(NetworkFunnelState);
    expect(restored.selection()).toEqual(EMPTY_FUNNEL_SELECTION);
  });
});
