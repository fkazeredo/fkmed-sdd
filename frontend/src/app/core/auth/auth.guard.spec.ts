import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

/** SPEC-0001 BR3/BR8: unauthenticated users are sent to the AS login (code flow). */
describe('authGuard', () => {
  function run(authenticated: boolean) {
    const auth = {
      isAuthenticated: () => authenticated,
      login: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: auth }],
    });
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    return { result, auth };
  }

  it('allows navigation when authenticated', () => {
    const { result, auth } = run(true);
    expect(result).toBe(true);
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('starts the code flow and blocks navigation when unauthenticated', () => {
    const { result, auth } = run(false);
    expect(result).toBe(false);
    expect(auth.login).toHaveBeenCalled();
  });
});
