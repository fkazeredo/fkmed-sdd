import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { ValidarBoleto } from './validar-boleto';

/** SPEC-0013 BR4: the antifraud validator — authentic vs not-recognized rendering, and the 47-digit
 * format error; the not-recognized branch MUST show the do-not-pay alert and never suggest paying. */
describe('ValidarBoleto', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ValidarBoleto],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(BeneficiaryContextService).active.set({
      beneficiaryId: 'maria-id',
      firstName: 'MARIA',
      role: 'TITULAR',
    });
  });

  afterEach(() => http.verify());

  function submit(line: string) {
    const fixture = TestBed.createComponent(ValidarBoleto);
    fixture.detectChanges();
    fixture.componentInstance['line'] = line;
    (fixture.nativeElement.querySelector('[data-testid="validar-submit"]') as HTMLElement).click();
    return fixture;
  }

  it('renders "Boleto autêntico" with competência and amount for an AUTHENTIC verdict', async () => {
    const fixture = submit('34191.09876 50004.320198 74561.230987 6 50000000000003');
    http.expectOne('/api/finance/invoices/validate').flush({
      result: 'AUTHENTIC',
      competencia: 'Julho/2026',
      dueDate: '2026-07-16',
      amount: 489.9,
    });
    await fixture.whenStable();
    fixture.detectChanges();

    const el = fixture.nativeElement.querySelector('[data-testid="validar-autentico"]');
    expect(el).toBeTruthy();
    expect(el.textContent).toContain('Julho/2026');
  });

  it('renders the mandatory do-not-pay alert for a NOT_RECOGNIZED verdict', async () => {
    const fixture = submit('11111111111111111111111111111111111111111111111');
    http.expectOne('/api/finance/invoices/validate').flush({ result: 'NOT_RECOGNIZED' });
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[data-testid="validar-nao-reconhecido"]');
    expect(alert).toBeTruthy();
    expect(alert.getAttribute('role')).toBe('alert');
    expect(alert.textContent).toContain('Não realize o pagamento');
    expect(fixture.nativeElement.querySelector('[data-testid="validar-autentico"]')).toBeFalsy();
  });

  it('shows the 47-digit format hint on a 422', async () => {
    const fixture = submit('123');
    http
      .expectOne('/api/finance/invoices/validate')
      .flush({ code: 'finance.line-invalid-format' }, { status: 422, statusText: 'Unprocessable Entity' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="validar-formato-erro"]')).toBeTruthy();
  });

  it('shows the friendly denial for a dependent', async () => {
    TestBed.inject(BeneficiaryContextService).active.set({
      beneficiaryId: 'pedro-id',
      firstName: 'PEDRO',
      role: 'DEPENDENT',
    });
    const fixture = TestBed.createComponent(ValidarBoleto);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="financas-denied"]')).toBeTruthy();
  });
});
