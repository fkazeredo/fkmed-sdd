import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { FinancasHub } from './financas-hub';
import { InvoiceSummary } from './finance.api';

const OPEN_TAB: InvoiceSummary[] = [
  {
    id: 'ov-1',
    competencia: 'Maio/2026',
    dueDate: '2026-05-31',
    amount: 489.9,
    status: 'OVERDUE',
    updatedAmount: { original: 489.9, fine: 9.8, interest: 0.98, daysOverdue: 6, total: 500.68 },
  },
  { id: 'op-1', competencia: 'Julho/2026', dueDate: '2026-07-16', amount: 489.9, status: 'OPEN' },
];
const PAID_TAB: InvoiceSummary[] = [
  { id: 'pa-1', competencia: 'Setembro/2025', dueDate: '2025-09-10', amount: 452.75, status: 'PAID', paidAt: '2025-09-08' },
];

/** SPEC-0013 BR1/BR2: the hub tabs (Em aberto = open + overdue with the valor-atualizado breakdown,
 * Pagos) and the titular-only denial. */
describe('FinancasHub', () => {
  let http: HttpTestingController;
  let context: BeneficiaryContextService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FinancasHub],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    context = TestBed.inject(BeneficiaryContextService);
    context.active.set({ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' });
  });

  afterEach(() => http.verify());

  it('shows the open + overdue invoices with the valor-atualizado breakdown', async () => {
    const fixture = TestBed.createComponent(FinancasHub);
    await fixture.whenStable();
    http.expectOne((r) => r.url === '/api/finance/invoices' && r.params.get('tab') === 'OPEN').flush(OPEN_TAB);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('[data-testid="financas-lista"] > li')).toHaveLength(2);
    const breakdown = fixture.nativeElement.querySelector('[data-testid="financas-atualizado"]');
    expect(breakdown).toBeTruthy();
    expect(breakdown.textContent).toContain('Atualize seu boleto pelos canais de atendimento');
  });

  it('re-fetches the PAID tab when selected', async () => {
    const fixture = TestBed.createComponent(FinancasHub);
    await fixture.whenStable();
    http.expectOne((r) => r.params.get('tab') === 'OPEN').flush(OPEN_TAB);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="financas-tab-pagos"]') as HTMLElement).click();
    await fixture.whenStable();
    http.expectOne((r) => r.params.get('tab') === 'PAID').flush(PAID_TAB);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="financas-pago-em"]')).toBeTruthy();
  });

  it('shows the friendly denial for a dependent, without querying the API', async () => {
    context.active.set({ beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' });
    const fixture = TestBed.createComponent(FinancasHub);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="financas-denied"]')).toBeTruthy();
  });
});
