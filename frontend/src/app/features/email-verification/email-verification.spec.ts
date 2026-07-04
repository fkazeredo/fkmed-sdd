import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { EmailVerification } from './email-verification';

/** SPEC-0002 e-mail verification landing (frontend): confirm from token, invalid→resend, neutral. */
describe('EmailVerification', () => {
  let http: HttpTestingController;

  function configure(queryParams: Record<string, string>): void {
    TestBed.configureTestingModule({
      imports: [EmailVerification],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        { provide: AuthService, useValue: { login: vi.fn() } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  }

  afterEach(() => http.verify());

  it('confirms the account from a valid link token', () => {
    configure({ token: 'raw-token' });
    const fixture = TestBed.createComponent(EmailVerification);
    fixture.detectChanges(); // triggers ngOnInit (zoneless)
    const request = http.expectOne('/api/auth/verification/confirm');
    expect(request.request.body).toEqual({ token: 'raw-token' });
    request.flush(null);
    expect(fixture.componentInstance.status()).toBe('confirmed');
  });

  it('shows the resend affordance when the link is invalid (410)', () => {
    configure({ token: 'expired' });
    const fixture = TestBed.createComponent(EmailVerification);
    fixture.detectChanges();
    http
      .expectOne('/api/auth/verification/confirm')
      .flush({ code: 'auth.verification-link-invalid' }, { status: 410, statusText: 'Gone' });
    expect(fixture.componentInstance.status()).toBe('invalid');
  });

  it('goes straight to the resend form when no token is present', () => {
    configure({});
    const fixture = TestBed.createComponent(EmailVerification);
    fixture.detectChanges();
    http.expectNone('/api/auth/verification/confirm');
    expect(fixture.componentInstance.status()).toBe('idle');
  });

  it('resend is neutral — the same outcome regardless of the server response (BR7)', () => {
    configure({});
    const fixture = TestBed.createComponent(EmailVerification);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.resendEmail = 'pedro@fkmed.local';
    component.resend();
    http
      .expectOne('/api/auth/verification/resend')
      .flush(null, { status: 500, statusText: 'Server Error' });
    expect(component.resendDone()).toBe(true);
  });
});
