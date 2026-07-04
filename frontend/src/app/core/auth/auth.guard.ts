import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { saveReturnUrl, takeReturnUrl } from './return-url';

/**
 * Redirects unauthenticated users to the AS login via the code flow (SPEC-0001 BR3/BR8),
 * preserving the attempted route (SPEC-0002 BR13). The same restore path serves BR12: the
 * session-expiry interceptor (`session-expiry.interceptor.ts`) saves the in-flight route the same
 * way before sending the user to the local notice, so this guard is the single place that
 * restores it once re-authenticated — no duplicated return-route logic (do not add another one).
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.isAuthenticated()) {
    const returnUrl = takeReturnUrl();
    if (returnUrl && returnUrl !== state.url) {
      return inject(Router).parseUrl(returnUrl);
    }
    return true;
  }
  saveReturnUrl(state.url);
  auth.login();
  return false;
};
