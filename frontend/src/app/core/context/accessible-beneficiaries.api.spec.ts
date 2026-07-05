import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AccessibleBeneficiariesApi, AccessibleBeneficiary } from './accessible-beneficiaries.api';

/** SPEC-0003: the selector's data source. Frozen contract — GET /api/context/accessible-beneficiaries. */
describe('AccessibleBeneficiariesApi', () => {
  let api: AccessibleBeneficiariesApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AccessibleBeneficiariesApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('calls GET /api/context/accessible-beneficiaries and returns the scoped list', () => {
    const payload: AccessibleBeneficiary[] = [
      { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' },
      { beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' },
    ];
    let result: AccessibleBeneficiary[] | undefined;
    api.getAccessibleBeneficiaries().subscribe((response) => (result = response));

    http.expectOne({ url: '/api/context/accessible-beneficiaries', method: 'GET' }).flush(payload);

    expect(result).toEqual(payload);
  });
});
