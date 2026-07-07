import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { BoletoDetail } from './boleto-detail';
import { InvoiceDetail } from './finance.api';

const OVERDUE_DETAIL: InvoiceDetail = {
  id: 'inv-1',
  competencia: 'Maio/2026',
  dueDate: '2026-05-31',
  amount: 489.9,
  status: 'OVERDUE',
  updatedAmount: { original: 489.9, fine: 9.8, interest: 0.98, daysOverdue: 6, total: 500.68 },
  digitableLine: '34191098765000432019874561230987650000000000003',
  pixCode: '00020126-pix-code-abc',
  barcodePayload: '34195000000000034191098765000432019874561230',
};

/** SPEC-0013 BR3: the boleto detail — copy the exact 47-digit line + the PIX code (with
 * confirmation), download the 2nd copy, and the titular-only denial (BR1). The detail is driven
 * through the component's signals (no route :id) — same approach as the SPEC-0011 detail spec. */
describe('BoletoDetail', () => {
  let http: HttpTestingController;
  let context: BeneficiaryContextService;
  const writeText = vi.fn().mockResolvedValue(undefined);

  beforeEach(async () => {
    Object.assign(navigator, { clipboard: { writeText } });
    writeText.mockClear();
    await TestBed.configureTestingModule({
      imports: [BoletoDetail],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    context = TestBed.inject(BeneficiaryContextService);
    context.active.set({ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' });
  });

  afterEach(() => http.verify());

  function withDetail(detail: InvoiceDetail): ComponentFixture<BoletoDetail> {
    const fixture = TestBed.createComponent(BoletoDetail);
    fixture.detectChanges();
    fixture.componentInstance['loading'].set(false);
    fixture.componentInstance['invoice'].set(detail);
    fixture.detectChanges();
    return fixture;
  }

  it('copies exactly the 47-digit digitable line with a confirmation', async () => {
    const fixture = withDetail(OVERDUE_DETAIL);
    (fixture.nativeElement.querySelector('[data-testid="boleto-copiar-linha"]') as HTMLElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(writeText).toHaveBeenCalledWith('34191098765000432019874561230987650000000000003');
    expect(writeText.mock.calls[0][0]).toHaveLength(47);
    expect(fixture.nativeElement.querySelector('[data-testid="boleto-linha-copiada"]')).toBeTruthy();
  });

  it('copies the PIX copia-e-cola code with a confirmation', async () => {
    const fixture = withDetail(OVERDUE_DETAIL);
    (fixture.nativeElement.querySelector('[data-testid="boleto-copiar-pix"]') as HTMLElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(writeText).toHaveBeenCalledWith('00020126-pix-code-abc');
    expect(fixture.nativeElement.querySelector('[data-testid="boleto-pix-copiado"]')).toBeTruthy();
  });

  it('renders the valor-atualizado breakdown for an overdue boleto', () => {
    const fixture = withDetail(OVERDUE_DETAIL);
    expect(fixture.nativeElement.querySelector('[data-testid="boleto-atualizado"]')).toBeTruthy();
  });

  it('downloads the 2nd-copy PDF from the blob endpoint', async () => {
    (URL as unknown as { createObjectURL: () => string }).createObjectURL = vi.fn().mockReturnValue('blob:x');
    (URL as unknown as { revokeObjectURL: () => void }).revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    const fixture = withDetail(OVERDUE_DETAIL);

    (fixture.nativeElement.querySelector('[data-testid="boleto-baixar-pdf"]') as HTMLElement).click();
    http.expectOne('/api/finance/invoices/inv-1/pdf').flush(new Blob(['%PDF'], { type: 'application/pdf' }));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="boleto-pdf-erro"]')).toBeFalsy();
  });

  it('shows the "não encontrado" state', () => {
    const fixture = TestBed.createComponent(BoletoDetail);
    fixture.detectChanges();
    fixture.componentInstance['loading'].set(false);
    fixture.componentInstance['notFound'].set(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="boleto-nao-encontrado"]')).toBeTruthy();
  });

  it('shows the friendly denial for a dependent, without querying the API', () => {
    context.active.set({ beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' });
    const fixture = TestBed.createComponent(BoletoDetail);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="financas-denied"]')).toBeTruthy();
  });
});
