import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { NotificationPreference } from '../../core/notifications/notifications.api';
import { NotificationPreferences } from './notification-preferences';

const OPTIONAL: NotificationPreference = {
  type: 'reimbursement.paid',
  description: 'Reembolso pago',
  emailOptOut: false,
  mandatory: false,
};
const MANDATORY: NotificationPreference = {
  type: 'auth.password-changed',
  description: 'Senha alterada',
  emailOptOut: false,
  mandatory: true,
};

/** SPEC-0004 BR7: preferences screen — e-mail opt-out per event type; mandatory types render
 * locked and reject the 422 defensively even though the UI never lets the user attempt it.
 * Real backend contract (OpenAPI snapshot): GET/PUT return the catalog wrapped in
 * `{ preferences: [...] }` with a `type` key; PUT is a batch that returns the updated catalog. */
describe('NotificationPreferences', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationPreferences],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function setup(): Promise<ComponentFixture<NotificationPreferences>> {
    const fixture = TestBed.createComponent(NotificationPreferences);
    await fixture.whenStable();
    return fixture;
  }

  function flushCatalog(catalog: NotificationPreference[]): void {
    http.expectOne({ url: '/api/notifications/preferences', method: 'GET' }).flush({ preferences: catalog });
  }

  it('shows the loading state before the catalog resolves', async () => {
    const fixture = await setup();
    expect(fixture.nativeElement.textContent).toContain('Carregando…');
    http.expectOne({ url: '/api/notifications/preferences', method: 'GET' }).flush({ preferences: [] });
  });

  it('shows the empty state when the catalog is empty', async () => {
    const fixture = await setup();
    flushCatalog([]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="notification-preferences-empty"]')).not.toBeNull();
  });

  it('shows an error state with retry on failure', async () => {
    const fixture = await setup();
    http
      .expectOne({ url: '/api/notifications/preferences', method: 'GET' })
      .flush({ code: 'internal.error' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar os dados. Tente novamente.');

    fixture.nativeElement.querySelector('button')?.click();
    flushCatalog([OPTIONAL]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="preference-reimbursement.paid"]')).not.toBeNull();
  });

  it('renders a mandatory type locked/disabled with a hint, and it cannot be toggled (BR7, AC4)', async () => {
    const fixture = await setup();
    flushCatalog([MANDATORY]);
    await fixture.whenStable();
    fixture.detectChanges();

    const toggle = fixture.nativeElement.querySelector(
      '[data-testid="preference-auth.password-changed-toggle"]',
    ) as HTMLButtonElement;
    expect(toggle.disabled).toBe(true);
    expect(
      fixture.nativeElement.querySelector('[data-testid="preference-auth.password-changed-mandatory-hint"]'),
    ).not.toBeNull();

    toggle.click();
    // No PUT is ever sent — the click is a no-op on a disabled/mandatory row.
  });

  it('toggling an optional type sends the batch PUT and reflects the returned catalog (label flips)', async () => {
    const fixture = await setup();
    flushCatalog([OPTIONAL]);
    await fixture.whenStable();
    fixture.detectChanges();

    const toggle = fixture.nativeElement.querySelector(
      '[data-testid="preference-reimbursement.paid-toggle"]',
    ) as HTMLButtonElement;
    expect(toggle.disabled).toBe(false);
    expect(toggle.textContent).toContain('E-mail ativado');

    toggle.click();
    const req = http.expectOne({ url: '/api/notifications/preferences', method: 'PUT' });
    expect(req.request.body).toEqual({ preferences: [{ type: 'reimbursement.paid', emailOptOut: true }] });
    // Real backend returns 200 + the updated catalog; the screen renders it verbatim.
    req.flush({ preferences: [{ ...OPTIONAL, emailOptOut: true }] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(toggle.textContent).toContain('E-mail desativado');
  });

  it('shows the mandatory-refusal error inline if the backend defensively rejects it (defensive path)', async () => {
    const fixture = await setup();
    flushCatalog([OPTIONAL]);
    await fixture.whenStable();
    fixture.detectChanges();

    const toggle = fixture.nativeElement.querySelector(
      '[data-testid="preference-reimbursement.paid-toggle"]',
    ) as HTMLButtonElement;
    toggle.click();
    http
      .expectOne({ url: '/api/notifications/preferences', method: 'PUT' })
      .flush({ code: 'notification.preference-mandatory' }, { status: 422, statusText: 'Unprocessable Entity' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="notification-preferences-error"]')).not.toBeNull();
  });
});
