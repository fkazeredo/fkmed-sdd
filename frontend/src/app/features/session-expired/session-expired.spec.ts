import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { SessionExpired } from './session-expired';

/**
 * BR12: local notice shown when the session-expiry interceptor redirects here after a mid-use
 * 401. "Entrar novamente" restarts the OIDC code flow; the guard restores the saved return route.
 */
describe('SessionExpired', () => {
  const login = vi.fn();

  beforeEach(async () => {
    login.mockClear();
    await TestBed.configureTestingModule({
      imports: [SessionExpired],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        { provide: AuthService, useValue: { login } },
      ],
    }).compileComponents();
  });

  it('shows the "Sua sessão expirou" notice', () => {
    const fixture = TestBed.createComponent(SessionExpired);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Sua sessão expirou');
  });

  it('restarts the code flow on "Entrar novamente"', () => {
    const fixture = TestBed.createComponent(SessionExpired);
    fixture.detectChanges();
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('[data-testid="session-expired-login"]')
      ?.click();
    expect(login).toHaveBeenCalled();
  });
});
