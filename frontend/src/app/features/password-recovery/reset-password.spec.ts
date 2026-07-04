import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { ResetPassword } from './reset-password';

/**
 * SPEC-0002 "Redefinir senha" (BR10/BR16, public route reached from the e-mailed link). Covers
 * AC5 (reused/expired link → 410 "invalid" state) and BR10's happy path (new password accepted).
 */
describe('ResetPassword', () => {
  let http: HttpTestingController;
  const login = vi.fn();

  function configure(queryParams: Record<string, string>): void {
    TestBed.configureTestingModule({
      imports: [ResetPassword],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        { provide: AuthService, useValue: { login } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  }

  beforeEach(() => login.mockClear());
  afterEach(() => http.verify());

  it('renders the form when a token is present', () => {
    configure({ token: 'raw-token' });
    const fixture = TestBed.createComponent(ResetPassword);
    fixture.detectChanges();
    expect(fixture.componentInstance.status()).toBe('form');
  });

  it('goes straight to the invalid state when there is no token', () => {
    configure({});
    const fixture = TestBed.createComponent(ResetPassword);
    fixture.detectChanges();
    http.expectNone('/api/auth/recovery/reset');
    expect(fixture.componentInstance.status()).toBe('invalid');
  });

  it('validates the password policy and confirmation client-side (BR16)', () => {
    configure({ token: 'raw-token' });
    const fixture = TestBed.createComponent(ResetPassword);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.newPassword = 'short';
    component.confirmPassword = 'short';
    expect(component.formValid).toBe(false);

    component.newPassword = 'Pedro1234';
    component.confirmPassword = 'Pedro1235';
    expect(component.confirmValid).toBe(false);
    expect(component.formValid).toBe(false);

    component.confirmPassword = 'Pedro1234';
    expect(component.formValid).toBe(true);
  });

  it('resets successfully and shows the success state (BR10)', () => {
    configure({ token: 'raw-token' });
    const fixture = TestBed.createComponent(ResetPassword);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.newPassword = 'Pedro1234';
    component.confirmPassword = 'Pedro1234';
    component.submit();

    const request = http.expectOne('/api/auth/recovery/reset');
    expect(request.request.body).toEqual({ token: 'raw-token', newPassword: 'Pedro1234' });
    request.flush(null);

    expect(component.status()).toBe('success');
  });

  it('a reused/expired link (AC5, 410) renders the invalid state', () => {
    configure({ token: 'used-token' });
    const fixture = TestBed.createComponent(ResetPassword);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.newPassword = 'Pedro1234';
    component.confirmPassword = 'Pedro1234';
    component.submit();

    http
      .expectOne('/api/auth/recovery/reset')
      .flush({ code: 'auth.reset-link-invalid' }, { status: 410, statusText: 'Gone' });

    expect(component.status()).toBe('invalid');
  });

  it('a server-side policy violation (race) shows an inline error, stays on the form', () => {
    configure({ token: 'raw-token' });
    const fixture = TestBed.createComponent(ResetPassword);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.newPassword = 'Pedro1234';
    component.confirmPassword = 'Pedro1234';
    component.submit();

    http
      .expectOne('/api/auth/recovery/reset')
      .flush({ code: 'auth.password-policy-violation' }, { status: 422, statusText: 'Unprocessable' });

    expect(component.status()).toBe('form');
    expect(component.errorKey()).toBe('redefinirSenha.erro.senhaFraca');
  });

  it('toggles password visibility', () => {
    configure({ token: 'raw-token' });
    const fixture = TestBed.createComponent(ResetPassword);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    expect(component.showPassword()).toBe(false);
    component.togglePassword();
    expect(component.showPassword()).toBe(true);
  });

  it('goes to login through the AuthService', () => {
    configure({ token: 'raw-token' });
    const fixture = TestBed.createComponent(ResetPassword);
    fixture.detectChanges();
    fixture.componentInstance.goToLogin();
    expect(login).toHaveBeenCalled();
  });
});
