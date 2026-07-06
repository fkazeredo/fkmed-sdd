import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  NetworkApi,
  ProviderDetail,
  ProviderSearchResult,
  RegistryOption,
  ServiceTypeOption,
} from './network.api';

/**
 * SPEC-0008 §API Contracts reconciled to the REAL backend (OpenAPI snapshot): the funnel lists
 * return raw arrays (no `{items:[…]}` envelope); only the provider search responses carry
 * `{referenceDate, items:[…]}`. Verifies each endpoint's path, the exact query parameters, and the
 * raw-array vs envelope shape.
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

  it('getStates() calls GET /api/network/states and returns a raw string[] of UF codes', () => {
    const result = ['RJ'];
    let received: string[] | undefined;
    api.getStates().subscribe((response) => (received = response));

    http.expectOne('/api/network/states').flush(result);
    expect(received).toEqual(result);
  });

  it('getMunicipalities() sends uf and, when given, q; returns a raw string[]', () => {
    let received: string[] | undefined;
    api.getMunicipalities('RJ', 'rio').subscribe((response) => (received = response));
    const req = http.expectOne((request) => request.url === '/api/network/municipalities');
    expect(req.request.params.get('uf')).toBe('RJ');
    expect(req.request.params.get('q')).toBe('rio');
    req.flush(['Rio de Janeiro', 'Rio Bonito']);
    expect(received).toEqual(['Rio de Janeiro', 'Rio Bonito']);
  });

  it('getMunicipalities() omits q when not given', () => {
    api.getMunicipalities('RJ').subscribe();
    const req = http.expectOne((request) => request.url === '/api/network/municipalities');
    expect(req.request.params.get('uf')).toBe('RJ');
    expect(req.request.params.has('q')).toBe(false);
    req.flush([]);
  });

  it('getNeighborhoods() sends uf and municipality; returns a raw string[]', () => {
    let received: string[] | undefined;
    api.getNeighborhoods('RJ', 'Rio de Janeiro').subscribe((response) => (received = response));
    const req = http.expectOne((request) => request.url === '/api/network/neighborhoods');
    expect(req.request.params.get('uf')).toBe('RJ');
    expect(req.request.params.get('municipality')).toBe('Rio de Janeiro');
    req.flush(['Centro', 'Copacabana', 'Tijuca']);
    expect(received).toEqual(['Centro', 'Copacabana', 'Tijuca']);
  });

  it('getServiceTypes() returns a raw ServiceTypeOption[] carrying hasSpecialtyStep', () => {
    const result: ServiceTypeOption[] = [
      { code: 'CONSULTORIOS', name: 'Consultórios–Clínicas–Terapias', hasSpecialtyStep: true },
      { code: 'LABORATORIOS', name: 'Laboratórios e Exames', hasSpecialtyStep: false },
    ];
    let received: ServiceTypeOption[] | undefined;
    api.getServiceTypes().subscribe((response) => (received = response));
    http.expectOne('/api/network/service-types').flush(result);
    expect(received).toEqual(result);
  });

  it('getSpecialties() returns a raw RegistryOption[]', () => {
    const result: RegistryOption[] = [{ code: 'CARDIOLOGIA', name: 'Cardiologia' }];
    let received: RegistryOption[] | undefined;
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

  it('getProviders() sends neighborhood and specialty when given; card carries a single locality (AC2)', () => {
    const result: ProviderSearchResult = {
      referenceDate: '2026-07-04',
      items: [
        {
          id: 'p1',
          name: 'Dr. João',
          locality: 'CENTRO, RIO DE JANEIRO – RJ',
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

  it('getProvider() calls GET /api/network/providers/{id} with the address/specialties shape', () => {
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
