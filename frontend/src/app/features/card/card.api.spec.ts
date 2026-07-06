import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CardApi, CardResponse } from './card.api';

/** SPEC-0007: frozen contract — GET /api/cards/{beneficiaryId} and its /pdf sibling. */
describe('CardApi', () => {
  let api: CardApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(CardApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('calls GET /api/cards/{beneficiaryId} and returns the card + data sheet', () => {
    const payload: CardResponse = {
      fullName: 'MARIA CLARA SOUZA LIMA',
      cardNumber: '001234567',
      cns: '700000000000001',
      ansRegistration: '326305',
      coverage: 'ESTADUAL',
      planName: 'PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP',
      planCategory: 'PRATA',
      additives: ['Urg/emerg Nacional Hr — Assistência'],
    };
    let result: CardResponse | undefined;
    api.getCard('maria-id').subscribe((response) => (result = response));

    http.expectOne({ url: '/api/cards/maria-id', method: 'GET' }).flush(payload);

    expect(result).toEqual(payload);
  });

  it('calls GET /api/cards/{beneficiaryId}/pdf as a blob (BR3 download)', () => {
    const pdfBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
    let result: Blob | undefined;
    api.downloadPdf('maria-id').subscribe((response) => (result = response));

    const req = http.expectOne({ url: '/api/cards/maria-id/pdf', method: 'GET' });
    expect(req.request.responseType).toBe('blob');
    req.flush(pdfBlob);

    expect(result).toEqual(pdfBlob);
  });
});
