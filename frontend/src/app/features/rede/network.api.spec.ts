import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  NetworkApi,
  ProviderDetail,
  ProviderSearchResult,
  RegistryOption,
  UfOption,
} from './network.api';

/**
 * SPEC-0008 §API Contracts (frozen contract, backend not yet integrated): every network-search
 * endpoint the frontend calls, and the exact query parameters each one sends.
 */
describe('NetworkApi', () => {
  let api: NetworkApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(NetworkApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getStates() calls GET /api/network/states', () => {
    const result: { items: UfOption[] } = { items: [{ code: 'RJ', name: 'Rio de Janeiro' }] };
    let received: { items: UfOption[] } | undefined;
    api.getStates().subscribe((response) => (received = response));

    http.expectOne('/api/network/states').flush(result);
    expect(received).toEqual(result);
  });

  it('getMunicipalities() sends uf and, when given, q', () => {
    api.getMunicipalities('RJ', 'rio').subscribe();
    const req = http.expectOne((request) => request.url === '/api/network/municipalities');
    expect(req.request.params.get('uf')).toBe('RJ');
    expect(req.request.params.get('q')).toBe('rio');
    req.flush({ items: ['Rio de Janeiro', 'Rio Bonito'] });
  });

  it('getMunicipalities() omits q when not given', () => {
    api.getMunicipalities('RJ').subscribe();
    const req = http.expectOne((request) => request.url === '/api/network/municipalities');
    expect(req.request.params.get('uf')).toBe('RJ');
    expect(req.request.params.has('q')).toBe(false);
    req.flush({ items: [] });
  });

  it('getNeighborhoods() sends uf and municipality', () => {
    api.getNeighborhoods('RJ', 'Rio de Janeiro').subscribe();
    const req = http.expectOne((request) => request.url === '/api/network/neighborhoods');
    expect(req.request.params.get('uf')).toBe('RJ');
    expect(req.request.params.get('municipality')).toBe('Rio de Janeiro');
    req.flush({ items: ['Centro', 'Copacabana', 'Tijuca'] });
  });

  it('getServiceTypes() calls GET /api/network/service-types', () => {
    const result: { items: RegistryOption[] } = {
      items: [{ code: 'CONSULTORIOS', name: 'Consultórios–Clínicas–Terapias' }],
    };
    let received: { items: RegistryOption[] } | undefined;
    api.getServiceTypes().subscribe((response) => (received = response));
    http.expectOne('/api/network/service-types').flush(result);
    expect(received).toEqual(result);
  });

  it('getSpecialties() calls GET /api/network/specialties', () => {
    const result: { items: RegistryOption[] } = { items: [{ code: 'CARDIOLOGIA', name: 'Cardiologia' }] };
    let received: { items: RegistryOption[] } | undefined;
    api.getSpecialties().subscribe((response) => (received = response));
    http.expectOne('/api/network/specialties').flush(result);
    expect(received).toEqual(result);
  });

  it('getProviders() sends every chosen filter, omitting neighborhood/specialty when absent', () => {
    api
      .getProviders({ uf: 'RJ', municipality: 'Rio de Janeiro', serviceType: 'CONSULTORIOS' })
      .subscribe();
    const req = http.expectOne((request) => request.url === '/api/network/providers');
    expect(req.request.params.get('uf')).toBe('RJ');
    expect(req.request.params.get('municipality')).toBe('Rio de Janeiro');
    expect(req.request.params.get('serviceType')).toBe('CONSULTORIOS');
    expect(req.request.params.has('neighborhood')).toBe(false);
    expect(req.request.params.has('specialty')).toBe(false);
    req.flush({ referenceDate: '2026-07-04', items: [] });
  });

  it('getProviders() sends neighborhood and specialty when given (AC2)', () => {
    const result: ProviderSearchResult = {
      referenceDate: '2026-07-04',
      items: [
        {
          id: 'p1',
          name: 'Dr. João',
          neighborhood: 'Centro',
          municipality: 'Rio de Janeiro',
          uf: 'RJ',
          serviceType: 'Consultórios–Clínicas–Terapias',
          seals: [],
        },
      ],
    };
    let received: ProviderSearchResult | undefined;
    api
      .getProviders({
        uf: 'RJ',
        municipality: 'Rio de Janeiro',
        neighborhood: 'Centro',
        serviceType: 'CONSULTORIOS',
        specialty: 'CARDIOLOGIA',
      })
      .subscribe((response) => (received = response));
    const req = http.expectOne((request) => request.url === '/api/network/providers');
    expect(req.request.params.get('neighborhood')).toBe('Centro');
    expect(req.request.params.get('specialty')).toBe('CARDIOLOGIA');
    req.flush(result);
    expect(received).toEqual(result);
  });

  it('searchProvidersByName() sends name and, when given, municipality (BR8)', () => {
    api.searchProvidersByName('cardio', 'Rio de Janeiro').subscribe();
    const req = http.expectOne((request) => request.url === '/api/network/providers/search');
    expect(req.request.params.get('name')).toBe('cardio');
    expect(req.request.params.get('municipality')).toBe('Rio de Janeiro');
    req.flush({ referenceDate: '2026-07-04', items: [] });
  });

  it('searchProvidersByName() omits municipality when not given', () => {
    api.searchProvidersByName('cardio').subscribe();
    const req = http.expectOne((request) => request.url === '/api/network/providers/search');
    expect(req.request.params.has('municipality')).toBe(false);
    req.flush({ referenceDate: '2026-07-04', items: [] });
  });

  it('getProvider() calls GET /api/network/providers/{id}', () => {
    const detail: ProviderDetail = {
      id: 'p1',
      name: 'Dr. João',
      serviceType: 'Consultórios–Clínicas–Terapias',
      specialties: ['Cardiologia'],
      address: {
        cep: '20000-000',
        street: 'Rua A',
        number: '10',
        complement: null,
        neighborhood: 'Centro',
        municipality: 'Rio de Janeiro',
        uf: 'RJ',
      },
      phone: '(21) 99999-0000',
      seals: [{ code: 'QUALI', name: 'Selo Qualidade', description: 'Descrição do selo' }],
    };
    let received: ProviderDetail | undefined;
    api.getProvider('p1').subscribe((response) => (received = response));
    http.expectOne('/api/network/providers/p1').flush(detail);
    expect(received).toEqual(detail);
  });
});
