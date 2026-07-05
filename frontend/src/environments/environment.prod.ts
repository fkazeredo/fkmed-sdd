/**
 * Production/E2E environment: SPA, API and Authorization Server share ONE origin behind the
 * reverse proxy (DECISIONS-BASELINE §0018), so the issuer is the page origin.
 */
export const environment = {
  production: true,
  oidcIssuer: window.location.origin,
  // SPEC-0006 BR10: product version from build config (kept in lockstep with backend/pom.xml
  // <version> by /release, DECISIONS-BASELINE §0015).
  appVersion: '0.6.0',
};
