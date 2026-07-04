/**
 * Production/E2E environment: SPA, API and Authorization Server share ONE origin behind the
 * reverse proxy (DECISIONS-BASELINE §0018), so the issuer is the page origin.
 */
export const environment = {
  production: true,
  oidcIssuer: window.location.origin,
};
