import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AccessibleBeneficiary } from './accessible-beneficiaries.api';
import { ACTIVE_BENEFICIARY_KEY, BeneficiaryContextService } from './beneficiary-context.service';

const MARIA: AccessibleBeneficiary = { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' };
const PEDRO: AccessibleBeneficiary = { beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' };

/** SPEC-0003 BR5: the active-beneficiary context — default selection, sessionStorage restore
 * and switching. The active beneficiary is client-side convenience only (BR3); the server is
 * the sole authority, re-validated on every request. */
describe('BeneficiaryContextService', () => {
  let service: BeneficiaryContextService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BeneficiaryContextService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('load() populates the accessible list and defaults the active one to the TITULAR', () => {
    service.load();
    http.expectOne('/api/context/accessible-beneficiaries').flush([PEDRO, MARIA]);

    expect(service.accessible()).toEqual([PEDRO, MARIA]);
    expect(service.active()).toEqual(MARIA);
  });

  it('defaults to the first entry when there is no TITULAR in scope (a dependent’s own session)', () => {
    service.load();
    http.expectOne('/api/context/accessible-beneficiaries').flush([PEDRO]);

    expect(service.active()).toEqual(PEDRO);
  });

  it('restores a previously chosen active beneficiary from sessionStorage', () => {
    sessionStorage.setItem(ACTIVE_BENEFICIARY_KEY, 'pedro-id');
    service.load();
    http.expectOne('/api/context/accessible-beneficiaries').flush([MARIA, PEDRO]);

    expect(service.active()).toEqual(PEDRO);
  });

  it('ignores a saved id that is no longer accessible and falls back to the TITULAR', () => {
    sessionStorage.setItem(ACTIVE_BENEFICIARY_KEY, 'someone-else');
    service.load();
    http.expectOne('/api/context/accessible-beneficiaries').flush([MARIA, PEDRO]);

    expect(service.active()).toEqual(MARIA);
  });

  it('setActive switches the active beneficiary and persists the id in sessionStorage', () => {
    service.load();
    http.expectOne('/api/context/accessible-beneficiaries').flush([MARIA, PEDRO]);

    service.setActive('pedro-id');

    expect(service.active()).toEqual(PEDRO);
    expect(sessionStorage.getItem(ACTIVE_BENEFICIARY_KEY)).toBe('pedro-id');
  });

  it('setActive ignores an id outside the accessible list', () => {
    service.load();
    http.expectOne('/api/context/accessible-beneficiaries').flush([MARIA]);

    service.setActive('unknown-id');

    expect(service.active()).toEqual(MARIA);
    expect(sessionStorage.getItem(ACTIVE_BENEFICIARY_KEY)).toBeNull();
  });
});
