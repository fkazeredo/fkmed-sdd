import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { BeneficiarySummary, BeneficiarySummaryApi } from './beneficiary-summary.api';

/** SPEC-0005 Home card data source. Frozen contract — GET /api/context/beneficiaries/{id}. */
describe('BeneficiarySummaryApi', () => {
  let api: BeneficiarySummaryApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(BeneficiarySummaryApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('calls GET /api/context/beneficiaries/{id} and returns the summary', () => {
    const payload: BeneficiarySummary = {
      firstName: 'MARIA',
      fullName: 'MARIA CLARA SOUZA LIMA',
      role: 'TITULAR',
      planName: 'ADESÃO PRATA RJ QP COPART TP',
      cardNumber: '001234567',
      avatarUrl: null,
    };
    let result: BeneficiarySummary | undefined;
    api.getBeneficiary('maria-id').subscribe((response) => (result = response));

    http.expectOne({ url: '/api/context/beneficiaries/maria-id', method: 'GET' }).flush(payload);

    expect(result).toEqual(payload);
  });
});
