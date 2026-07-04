import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { Security } from './security';

/**
 * SPEC-0002 "Segurança" (BR11/BR16, authenticated route, AC-8): change password with the current
 * password required, read-only login e-mail, and the mobile-biometrics info card (no backend).
 */
describe('Security', () => {
  let http: HttpTestingController;
  const auth = { username: () => 'maria@fkmed.local', logout: vi.fn() };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Security],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        { provide: AuthService, useValue: auth },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('shows the read-only login e-mail', () => {
    const fixture = TestBed.createComponent(Security);
    fixture.detectChanges();
    expect(fixture.componentInstance.email()).toBe('maria@fkmed.local');
  });

  it('validates the new password policy and confirmation client-side (BR16)', () => {
    const fixture = TestBed.createComponent(Security);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.currentPassword = 'Maria1234';

    component.newPassword = 'short';
    component.confirmPassword = 'short';
    expect(component.formValid).toBe(false);

    component.newPassword = 'Maria4321';
    component.confirmPassword = 'Maria4321';
    expect(component.formValid).toBe(true);
  });

  it('rejects a new password equal to the current one (BR9 "differ from current")', () => {
    const fixture = TestBed.createComponent(Security);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.currentPassword = 'Maria1234';
    component.newPassword = 'Maria1234';
    component.confirmPassword = 'Maria1234';
    expect(component.formValid).toBe(false);
  });

  it('changes the password successfully', () => {
    const fixture = TestBed.createComponent(Security);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.currentPassword = 'Maria1234';
    component.newPassword = 'Maria4321';
    component.confirmPassword = 'Maria4321';
    component.submit();

    const request = http.expectOne('/api/auth/password');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      currentPassword: 'Maria1234',
      newPassword: 'Maria4321',
    });
    request.flush(null);

    expect(component.success()).toBe(true);
    expect(component.errorKey()).toBeNull();
    // Fields are cleared after a successful change — nothing lingers in memory.
    expect(component.currentPassword).toBe('');
    expect(component.newPassword).toBe('');
    expect(component.confirmPassword).toBe('');
  });

  it('shows the wrong-current-password message next to the field (422)', () => {
    const fixture = TestBed.createComponent(Security);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.currentPassword = 'wrong-current';
    component.newPassword = 'Maria4321';
    component.confirmPassword = 'Maria4321';
    component.submit();

    http
      .expectOne('/api/auth/password')
      .flush(
        { code: 'auth.current-password-incorrect' },
        { status: 422, statusText: 'Unprocessable' },
      );

    expect(component.errorKey()).toBe('seguranca.erro.senhaAtualIncorreta');
    expect(component.errorField()).toBe('currentPassword');
    expect(component.success()).toBe(false);
  });

  it('shows the weak-password message next to the new-password field (422)', () => {
    const fixture = TestBed.createComponent(Security);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.currentPassword = 'Maria1234';
    component.newPassword = 'Maria4321';
    component.confirmPassword = 'Maria4321';
    component.submit();

    http
      .expectOne('/api/auth/password')
      .flush(
        { code: 'auth.password-policy-violation' },
        { status: 422, statusText: 'Unprocessable' },
      );

    expect(component.errorKey()).toBe('seguranca.erro.senhaFraca');
    expect(component.errorField()).toBe('newPassword');
  });

  it('toggles show/hide independently for current and new password', () => {
    const fixture = TestBed.createComponent(Security);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    expect(component.showCurrent()).toBe(false);
    expect(component.showNew()).toBe(false);
    component.toggleCurrent();
    expect(component.showCurrent()).toBe(true);
    expect(component.showNew()).toBe(false);
    component.toggleNew();
    expect(component.showNew()).toBe(true);
  });
});
