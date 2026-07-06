import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GuideCard, GuideDetail, GuidesApi, TokenResponse } from './guias.api';

/**
 * SPEC-0012 §API Contracts — aligned to the real backend at integration: the list is a raw JSON
 * array (no `{items:[…]}` envelope), the period filter is a 3-value enum (`LAST_30D|LAST_90D|
 * LAST_12M`, no custom range), and the detail endpoint requires `beneficiaryId`. Verifies the exact
 * query parameters / body sent, and that responses pass through untouched.
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
    req.flush([]);

    api.getGuides({ beneficiaryId: 'maria-id', status: 'NEGADA', period: 'LAST_90D' }).subscribe();
    const req2 = http.expectOne((request) => request.url === '/api/guides');
    expect(req2.request.params.get('status')).toBe('NEGADA');
    expect(req2.request.params.get('period')).toBe('LAST_90D');
    req2.flush([]);
  });

  it('getGuides() returns the raw array exactly as received (no {items} envelope)', () => {
    const result: GuideCard[] = [
      {
        id: 'guide-1',
        number: 'GU-0001',
        type: 'CONSULTA',
        requestingProvider: 'Dr. João',
        requestedAt: '2026-07-01',
        status: 'EM_ANALISE',
      },
    ];
    let received: GuideCard[] | undefined;
    api.getGuides({ beneficiaryId: 'maria-id' }).subscribe((r) => (received = r));
    http.expectOne('/api/guides?beneficiaryId=maria-id').flush(result);
    expect(received).toEqual(result);
  });

  it('getGuide() calls GET /api/guides/{id} with the required beneficiaryId param', () => {
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
    api.getGuide('guide-1', 'maria-id').subscribe((r) => (received = r));
    const req = http.expectOne((request) => request.url === '/api/guides/guide-1');
    expect(req.request.params.get('beneficiaryId')).toBe('maria-id');
    req.flush(detail);
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
