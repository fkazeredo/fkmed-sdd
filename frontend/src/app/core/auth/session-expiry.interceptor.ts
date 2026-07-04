import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { saveReturnUrl } from './return-url';

/**
 * BR12: a session expiring mid-use surfaces to the SPA as a 401 on the stateless `/api/**` chain
 * (ADR-0005) — there is no other signal, since `/api/**` never redirects. On that 401, save the
 * in-flight route (the `authGuard` restores it once the user is re-authenticated — one restore
 * path, see its docstring) and send the user to the local "Sua sessão expirou" notice instead of
 * leaving them on a broken screen. Ignores non-API calls (the OIDC library's own requests) and a
 * 401 that happens while already on the notice screen, which would otherwise loop.
 */
export const sessionExpiryInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        req.url.startsWith('/api/') &&
        router.url !== '/sessao-expirada'
      ) {
        saveReturnUrl(router.url);
        router.navigateByUrl('/sessao-expirada');
      }
      return throwError(() => error);
    }),
  );
};
