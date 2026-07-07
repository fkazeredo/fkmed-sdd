import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { ImpostoRenda } from './imposto-renda';
import { Quitacao } from './quitacao';

/** SPEC-0013 BR6/BR7: the IR statement years and the Lei 12.007 settlement years, each with its PDF
 * download, the empty/guidance state, and the titular-only denial. */
describe('IR and settlement statements', () => {
  let http: HttpTestingController;
  let context: BeneficiaryContextService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImpostoRenda, Quitacao],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    context = TestBed.inject(BeneficiaryContextService);
    context.active.set({ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' });
  });

  afterEach(() => http.verify());

  it('lists IR base years and downloads a statement PDF', async () => {
    (URL as unknown as { createObjectURL: () => string }).createObjectURL = vi.fn().mockReturnValue('blob:x');
    (URL as unknown as { revokeObjectURL: () => void }).revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    const fixture = TestBed.createComponent(ImpostoRenda);
    await fixture.whenStable();
    http.expectOne('/api/finance/tax-statements').flush([{ year: 2025 }]);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="ir-baixar-2025"]') as HTMLElement).click();
    http.expectOne('/api/finance/tax-statements/2025/pdf').flush(new Blob(['%PDF'], { type: 'application/pdf' }));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="ir-pdf-erro"]')).toBeFalsy();
  });

  it('shows the IR empty state when there are no base years', async () => {
    const fixture = TestBed.createComponent(ImpostoRenda);
    await fixture.whenStable();
    http.expectOne('/api/finance/tax-statements').flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="ir-vazio"]')).toBeTruthy();
  });

  it('lists settlement years and downloads a declaration PDF', async () => {
    (URL as unknown as { createObjectURL: () => string }).createObjectURL = vi.fn().mockReturnValue('blob:x');
    (URL as unknown as { revokeObjectURL: () => void }).revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    const fixture = TestBed.createComponent(Quitacao);
    await fixture.whenStable();
    http.expectOne('/api/finance/settlement-declarations').flush([{ year: 2025 }]);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="quitacao-baixar-2025"]') as HTMLElement).click();
    http.expectOne('/api/finance/settlement-declarations/2025/pdf').flush(new Blob(['%PDF'], { type: 'application/pdf' }));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="quitacao-pdf-erro"]')).toBeFalsy();
  });

  it('shows the settlement guidance/empty state when no year is fully paid', async () => {
    const fixture = TestBed.createComponent(Quitacao);
    await fixture.whenStable();
    http.expectOne('/api/finance/settlement-declarations').flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="quitacao-vazio"]')).toBeTruthy();
  });

  it('denies both statement screens for a dependent, without querying', async () => {
    context.active.set({ beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' });
    const ir = TestBed.createComponent(ImpostoRenda);
    await ir.whenStable();
    ir.detectChanges();
    const settlement = TestBed.createComponent(Quitacao);
    await settlement.whenStable();
    settlement.detectChanges();

    expect(ir.nativeElement.querySelector('[data-testid="financas-denied"]')).toBeTruthy();
    expect(settlement.nativeElement.querySelector('[data-testid="financas-denied"]')).toBeTruthy();
  });
});
