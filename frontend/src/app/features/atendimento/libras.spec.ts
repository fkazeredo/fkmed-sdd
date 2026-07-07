import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { Libras } from './libras';

/** SPEC-0014 BR4/AC4: registering a Libras request for the active beneficiary confirms an
 * imminent videocall within hours, or the next operating period (with the hours) outside them. */
describe('Libras', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Libras],
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

  it('registers the request and confirms the imminent videocall when within hours', async () => {
    const fixture = TestBed.createComponent(Libras);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="libras-solicitar"]') as HTMLElement).click();
    const request = http.expectOne('/api/support/libras-requests');
    expect(request.request.body).toEqual({ beneficiaryId: 'maria-id' });
    request.flush({ situation: 'REGISTERED', nextStep: 'videocall-shortly' });
    await fixture.whenStable();
    fixture.detectChanges();

    const confirmation = fixture.nativeElement.querySelector('[data-testid="libras-confirmacao"]');
    expect(confirmation?.textContent).toContain('Nossa equipe iniciará a videochamada em instantes');
  });

  it('shows the next-operating-period confirmation with the hours when outside hours', async () => {
    const fixture = TestBed.createComponent(Libras);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="libras-solicitar"]') as HTMLElement).click();
    http
      .expectOne('/api/support/libras-requests')
      .flush({ situation: 'REGISTERED', nextStep: 'next-period', hoursStart: '08:00', hoursEnd: '18:00' });
    await fixture.whenStable();
    fixture.detectChanges();

    const confirmation = fixture.nativeElement.querySelector('[data-testid="libras-confirmacao"]');
    expect(confirmation?.textContent).toContain('08:00');
    expect(confirmation?.textContent).toContain('18:00');
  });

  it('shows a generic error when the request fails', async () => {
    const fixture = TestBed.createComponent(Libras);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="libras-solicitar"]') as HTMLElement).click();
    http
      .expectOne('/api/support/libras-requests')
      .flush({ code: 'x' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar os dados');
  });
});
