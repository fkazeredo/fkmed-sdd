import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GuideDetail, GuideListResponse, GuidesApi, TokenResponse } from './guias.api';

/**
 * SPEC-0012 §API Contracts — built against the spec's own literal examples (the slice plan's
 * frozen-contract doc was not present in the repo — see this dev's report). Verifies the exact
 * query parameters / body sent, and that responses pass through untouched — no invented envelope.
 */
describe('GuidesApi', () => {
  let api: GuidesApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(GuidesApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getGuides() always sends beneficiaryId, and status/period only when given', () => {
    api.getGuides({ beneficiaryId: 'maria-id' }).subscribe();
    const req = http.expectOne((request) => request.url === '/api/guides');
    expect(req.request.params.get('beneficiaryId')).toBe('maria-id');
    expect(req.request.params.has('status')).toBe(false);
    expect(req.request.params.has('period')).toBe(false);
    req.flush({ items: [] });

    api.getGuides({ beneficiaryId: 'maria-id', status: 'NEGADA', period: 'P90D' }).subscribe();
    const req2 = http.expectOne((request) => request.url === '/api/guides');
    expect(req2.request.params.get('status')).toBe('NEGADA');
    expect(req2.request.params.get('period')).toBe('P90D');
    req2.flush({ items: [] });
  });

  it('getGuides() sends from/to instead of period for a custom range', () => {
    api.getGuides({ beneficiaryId: 'maria-id', from: '2026-01-01', to: '2026-07-01' }).subscribe();
    const req = http.expectOne((request) => request.url === '/api/guides');
    expect(req.request.params.has('period')).toBe(false);
    expect(req.request.params.get('from')).toBe('2026-01-01');
    expect(req.request.params.get('to')).toBe('2026-07-01');
    req.flush({ items: [] });
  });

  it('getGuides() returns the bare {items:[...]} envelope exactly as received', () => {
    const result: GuideListResponse = {
      items: [
        {
          id: 'guide-1',
          number: 'GU-0001',
          type: 'CONSULTA',
          requestingProvider: 'Dr. João',
          requestedAt: '2026-07-01',
          status: 'EM_ANALISE',
        },
      ],
    };
    let received: GuideListResponse | undefined;
    api.getGuides({ beneficiaryId: 'maria-id' }).subscribe((r) => (received = r));
    http.expectOne('/api/guides?beneficiaryId=maria-id').flush(result);
    expect(received).toEqual(result);
  });

  it('getGuide() calls GET /api/guides/{id}', () => {
    const detail: GuideDetail = {
      id: 'guide-1',
      number: 'GU-0001',
      type: 'CONSULTA',
      requestingProvider: 'Dr. João',
      requestedAt: '2026-07-01',
      status: 'AUTORIZADA',
      items: [{ tussCode: '10101012', description: 'Consulta médica', quantity: 1, status: 'AUTORIZADO' }],
      authPassword: 'AUT-482913',
      authValidUntil: '2026-08-03',
    };
    let received: GuideDetail | undefined;
    api.getGuide('guide-1').subscribe((r) => (received = r));
    http.expectOne('/api/guides/guide-1').flush(detail);
    expect(received).toEqual(detail);
  });

  it('getCurrentToken() sends beneficiaryId and returns the token response', () => {
    const token: TokenResponse = { code: '483920', expiresAt: '2026-07-06T10:10:00Z' };
    let received: TokenResponse | undefined;
    api.getCurrentToken('maria-id').subscribe((r) => (received = r));
    const req = http.expectOne((request) => request.url === '/api/tokens/current');
    expect(req.request.params.get('beneficiaryId')).toBe('maria-id');
    req.flush(token);
    expect(received).toEqual(token);
  });

  it('generateToken() posts {beneficiaryId} to /api/tokens', () => {
    const token: TokenResponse = { code: '111222', expiresAt: '2026-07-06T10:20:00Z' };
    let received: TokenResponse | undefined;
    api.generateToken('maria-id').subscribe((r) => (received = r));
    const req = http.expectOne({ url: '/api/tokens', method: 'POST' });
    expect(req.request.body).toEqual({ beneficiaryId: 'maria-id' });
    req.flush(token);
    expect(received).toEqual(token);
  });
});
