import { inject } from '@angular/core';
import { CanActivateChildFn, Router, UrlTree } from '@angular/router';
import { catchError, map, Observable, of } from 'rxjs';
import { LegalDocumentsService } from './legal-documents.service';

/** The single legal-acceptance route the guard funnels the user to while a version is pending. */
export const LEGAL_ACCEPTANCE_PATH = '/aceite-legal';

/**
 * Terms interception (SPEC-0006 BR8): a CanActivateChild on the authenticated shell that, once a
 * new mandatory version is unaccepted, blocks *every* internal route and redirects to the
 * acceptance screen until the user clicks "Li e aceito". Only Sair stays reachable — it is the
 * shell header's logout button, not a route, so the guard never touches it.
 *
 * - pending + navigating elsewhere → redirect to the acceptance screen;
 * - pending + already on it → allow (it is the one place the user may be);
 * - nothing pending + trying the acceptance screen → send home (no reason to show it);
 * - nothing pending elsewhere → allow.
 *
 * The check fails *open*: if the snapshot cannot be loaded we must not lock the user out of the
 * whole app over a transient error — a real pending version will intercept on the next navigation
 * once the endpoint recovers.
 */
export const legalAcceptanceGuard: CanActivateChildFn = (
  _childRoute,
  state,
): Observable<boolean | UrlTree> => {
  const legal = inject(LegalDocumentsService);
  const router = inject(Router);
  const onAcceptanceScreen = state.url.startsWith(LEGAL_ACCEPTANCE_PATH);

  return legal.ensureLoaded().pipe(
    map((): boolean | UrlTree => {
      if (legal.hasPending()) {
        return onAcceptanceScreen ? true : router.parseUrl(LEGAL_ACCEPTANCE_PATH);
      }
      return onAcceptanceScreen ? router.parseUrl('/home') : true;
    }),
    catchError(() => of(true)),
  );
};
