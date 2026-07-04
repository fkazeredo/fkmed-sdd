import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { ForgotPassword } from './forgot-password';

/**
 * SPEC-0002 "Esqueci minha senha" (BR7/BR10/AC8): the neutral confirmation renders identically
 * whether the e-mail exists or not — the UI itself must not become an enumeration oracle either.
 */
describe('ForgotPassword', () => {
  let http: HttpTestingController;
  const login = vi.fn();

  beforeEach(async () => {
    login.mockClear();
    await TestBed.configureTestingModule({
      imports: [ForgotPassword],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        { provide: AuthService, useValue: { login } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function submit(component: ForgotPassword, email: string): void {
    component.email = email;
    component.submit();
  }

  it('shows the neutral confirmation on success (existing e-mail, AC8)', () => {
    const fixture = TestBed.createComponent(ForgotPassword);
    const component = fixture.componentInstance;
    submit(component, 'maria@fkmed.local');
    const request = http.expectOne('/api/auth/recovery/request');
    expect(request.request.body).toEqual({ email: 'maria@fkmed.local' });
    request.flush(null, { status: 202, statusText: 'Accepted' });

    expect(component.done()).toBe(true);
  });

  it('shows the SAME neutral confirmation even when the server errors (AC8 neutrality)', () => {
    const fixture = TestBed.createComponent(ForgotPassword);
    const component = fixture.componentInstance;
    submit(component, 'nao-cadastrado@fkmed.local');
    http
      .expectOne('/api/auth/recovery/request')
      .flush(null, { status: 500, statusText: 'Server Error' });

    expect(component.done()).toBe(true);
  });

  it('does not submit an invalid e-mail', () => {
    const fixture = TestBed.createComponent(ForgotPassword);
    const component = fixture.componentInstance;
    submit(component, 'not-an-email');
    http.expectNone('/api/auth/recovery/request');
    expect(component.done()).toBe(false);
  });

  it('goes to login through the AuthService', () => {
    const fixture = TestBed.createComponent(ForgotPassword);
    fixture.componentInstance.goToLogin();
    expect(login).toHaveBeenCalled();
  });
});
