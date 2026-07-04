import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { saveReturnUrl, takeReturnUrl } from './return-url';

/**
 * SPEC-0002 BR13 (visitor sent to login, returns to the original route) and BR12 (session expiry
 * uses the same restore path — DL: reuse the guard instead of duplicating the return-route logic
 * in the AuthService).
 */
describe('authGuard', () => {
  beforeEach(() => sessionStorage.clear());

  function run(authenticated: boolean, url: string) {
    const auth = {
      isAuthenticated: () => authenticated,
      login: vi.fn(),
    };
    const urlTree = {} as UrlTree;
    const router = {
      parseUrl: vi.fn().mockReturnValue(urlTree),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );
    return { result, auth, router, urlTree };
  }

  it('allows navigation when authenticated and nothing was pending', () => {
    const { result, auth } = run(true, '/meu-plano');
    expect(result).toBe(true);
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('starts the code flow and saves the attempted route when unauthenticated (BR13)', () => {
    const { result, auth } = run(false, '/seguranca');
    expect(result).toBe(false);
    expect(auth.login).toHaveBeenCalled();
    expect(takeReturnUrl()).toBe('/seguranca');
  });

  it('restores the saved return route once authenticated (BR12/BR13 round trip)', () => {
    saveReturnUrl('/seguranca');
    const { result, router, urlTree } = run(true, '/meu-plano');
    expect(router.parseUrl).toHaveBeenCalledWith('/seguranca');
    expect(result).toBe(urlTree);
    // Consumed — a second navigation does not redirect again.
    expect(takeReturnUrl()).toBeNull();
  });

  it('does not redirect when the saved route is the one already being activated', () => {
    saveReturnUrl('/meu-plano');
    const { result, router } = run(true, '/meu-plano');
    expect(result).toBe(true);
    expect(router.parseUrl).not.toHaveBeenCalled();
  });
});
