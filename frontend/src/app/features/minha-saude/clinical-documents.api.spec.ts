import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ClinicalDocumentDetail, ClinicalDocumentListResponse, ClinicalDocumentsApi } from './clinical-documents.api';

/**
 * SPEC-0011 §API Contracts — FROZEN: `GET /api/clinical-documents` (bare `{items:[...]}`
 * envelope, `category`/`beneficiaryId`/`period` or custom `from`/`to`), `GET
 * /api/clinical-documents/{id}` (type-specific detail) and `GET /api/clinical-documents/{id}/pdf`
 * (blob). Verifies the exact query parameters sent — no invented envelope.
 */
describe('ClinicalDocumentsApi', () => {
  let api: ClinicalDocumentsApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ClinicalDocumentsApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getDocuments() always sends category and beneficiaryId (default "all")', () => {
    api.getDocuments({ category: 'PRESCRIPTION', beneficiaryId: 'all', period: 'P30D' }).subscribe();
    const req = http.expectOne((request) => request.url === '/api/clinical-documents');
    expect(req.request.params.get('category')).toBe('PRESCRIPTION');
    expect(req.request.params.get('beneficiaryId')).toBe('all');
    expect(req.request.params.get('period')).toBe('P30D');
    expect(req.request.params.has('from')).toBe(false);
    expect(req.request.params.has('to')).toBe(false);
    req.flush({ items: [] });
  });

  it('getDocuments() sends a specific beneficiaryId when filtering by one beneficiary', () => {
    api.getDocuments({ category: 'EXAM_ORDER', beneficiaryId: 'pedro-id', period: 'P90D' }).subscribe();
    const req = http.expectOne((request) => request.url === '/api/clinical-documents');
    expect(req.request.params.get('beneficiaryId')).toBe('pedro-id');
    req.flush({ items: [] });
  });

  it('getDocuments() sends from/to instead of period for a custom range', () => {
    api
      .getDocuments({ category: 'REFERRAL', beneficiaryId: 'all', from: '2026-01-01', to: '2026-07-01' })
      .subscribe();
    const req = http.expectOne((request) => request.url === '/api/clinical-documents');
    expect(req.request.params.has('period')).toBe(false);
    expect(req.request.params.get('from')).toBe('2026-01-01');
    expect(req.request.params.get('to')).toBe('2026-07-01');
    req.flush({ items: [] });
  });

  it('getDocuments() returns the bare {items:[...]} envelope exactly as received', () => {
    const result: ClinicalDocumentListResponse = {
      items: [
        {
          id: 'doc-1',
          type: 'PRESCRIPTION',
          professional: 'Dra. Ana Souza',
          crm: 'CRM 12345 RJ',
          issuedAt: '2026-07-04',
          beneficiary: 'PEDRO',
          validUntil: '2026-08-03',
          expired: false,
        },
      ],
    };
    let received: ClinicalDocumentListResponse | undefined;
    api.getDocuments({ category: 'PRESCRIPTION', beneficiaryId: 'all', period: 'P30D' }).subscribe((r) => (received = r));
    http.expectOne('/api/clinical-documents?category=PRESCRIPTION&beneficiaryId=all&period=P30D').flush(result);
    expect(received).toEqual(result);
  });

  it('getDocument() calls GET /api/clinical-documents/{id}', () => {
    const detail: ClinicalDocumentDetail = {
      id: 'doc-1',
      type: 'SICK_NOTE',
      professional: 'Dr. João',
      crm: 'CRM 54321 RJ',
      issuedAt: '2026-07-01',
      beneficiary: 'MARIA',
      validUntil: null,
      expired: false,
      periodStart: '2026-07-01',
      periodEnd: '2026-07-03',
      cid: 'J11',
      notes: 'Repouso domiciliar.',
    };
    let received: ClinicalDocumentDetail | undefined;
    api.getDocument('doc-1').subscribe((r) => (received = r));
    http.expectOne('/api/clinical-documents/doc-1').flush(detail);
    expect(received).toEqual(detail);
  });

  it('downloadPdf() requests a blob from GET /api/clinical-documents/{id}/pdf', () => {
    const pdfBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
    let received: Blob | undefined;
    api.downloadPdf('doc-1').subscribe((response) => (received = response));
    const req = http.expectOne('/api/clinical-documents/doc-1/pdf');
    expect(req.request.responseType).toBe('blob');
    req.flush(pdfBlob);
    expect(received).toEqual(pdfBlob);
  });
});
