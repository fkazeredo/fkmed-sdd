const RETURN_URL_KEY = 'fkmed.returnUrl';

/** Routes that are never worth "returning to" — saving them would be a no-op or a loop. */
const NOT_WORTH_SAVING = new Set(['/', '/sessao-expirada']);

/**
 * Preserves the route a visitor was denied (BR13) or was on when the session expired mid-use
 * (BR12) across the full-page redirect to the AS login and back. `sessionStorage` is the right
 * store: it survives that round trip (same tab) but never leaks across tabs or into a later
 * session (docs/architecture/frontend-angular.md — no global store for this kind of transient
 * UI state).
 */
export function saveReturnUrl(url: string): void {
  if (url && !NOT_WORTH_SAVING.has(url)) {
    sessionStorage.setItem(RETURN_URL_KEY, url);
  }
}

/** Reads and clears the saved return route; a single restore per redirect round trip. */
export function takeReturnUrl(): string | null {
  const url = sessionStorage.getItem(RETURN_URL_KEY);
  if (url !== null) {
    sessionStorage.removeItem(RETURN_URL_KEY);
  }
  return url;
}
