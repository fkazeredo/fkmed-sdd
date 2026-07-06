import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { Coparticipacao } from './coparticipacao';
import { CopayStatement } from './finance.api';

const FAMILY: CopayStatement = {
  entries: [
    { date: '2026-07-01', procedure: 'Consulta', provider: 'Clínica A', beneficiaryName: 'MARIA', amount: 35 },
    { date: '2026-06-20', procedure: 'Fisioterapia', provider: 'Clínica B', beneficiaryName: 'PEDRO', amount: 25 },
  ],
  total: 60,
};
const PEDRO_ONLY: CopayStatement = {
  entries: [
    { date: '2026-06-20', procedure: 'Fisioterapia', provider: 'Clínica B', beneficiaryName: 'PEDRO', amount: 25 },
  ],
  total: 25,
};

/** SPEC-0013 BR5: the copay statement recalculates entries + total on every filter change (period /
 * beneficiary), and shows the empty state. */
describe('Coparticipacao', () => {
  let http: HttpTestingController;
  let context: BeneficiaryContextService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Coparticipacao],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    context = TestBed.inject(BeneficiaryContextService);
    context.accessible.set([
      { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' },
      { beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' },
    ]);
    context.active.set({ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' });
  });

  afterEach(() => http.verify());

  it('loads the current month on init and totals the returned entries', async () => {
    const fixture = TestBed.createComponent(Coparticipacao);
    await fixture.whenStable();
    const req = http.expectOne((r) => r.url === '/api/finance/copay' && r.params.get('period') === 'CURRENT_MONTH');
    req.flush(FAMILY);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="copay-total"]').textContent).toContain('60,00');
    expect(fixture.nativeElement.querySelectorAll('[data-testid="copay-tabela"] tbody tr')).toHaveLength(2);
  });

  it('recalculates when the period filter changes', async () => {
    const fixture = TestBed.createComponent(Coparticipacao);
    await fixture.whenStable();
    http.expectOne((r) => r.params.get('period') === 'CURRENT_MONTH').flush(FAMILY);
    fixture.detectChanges();

    fixture.componentInstance.onPeriodChange('LAST_3M');
    await fixture.whenStable();
    http.expectOne((r) => r.params.get('period') === 'LAST_3M').flush(FAMILY);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="copay-total"]')).toBeTruthy();
  });

  it('narrows to one beneficiary when the beneficiary filter changes', async () => {
    const fixture = TestBed.createComponent(Coparticipacao);
    await fixture.whenStable();
    http.expectOne((r) => r.params.get('period') === 'CURRENT_MONTH').flush(FAMILY);
    fixture.detectChanges();

    fixture.componentInstance.onBeneficiaryChange('pedro-id');
    await fixture.whenStable();
    const req = http.expectOne((r) => r.url === '/api/finance/copay' && r.params.get('beneficiaryId') === 'pedro-id');
    req.flush(PEDRO_ONLY);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="copay-total"]').textContent).toContain('25,00');
    expect(fixture.nativeElement.querySelectorAll('[data-testid="copay-tabela"] tbody tr')).toHaveLength(1);
  });

  it('shows the empty state when there are no entries', async () => {
    const fixture = TestBed.createComponent(Coparticipacao);
    await fixture.whenStable();
    http.expectOne((r) => r.params.get('period') === 'CURRENT_MONTH').flush({ entries: [], total: 0 });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="copay-vazio"]')).toBeTruthy();
  });
});
