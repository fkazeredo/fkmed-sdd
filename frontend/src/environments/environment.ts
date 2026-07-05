/**
 * Dev environment: the SPA runs on ng serve (:4200) and talks to the backend origin (:8080)
 * for OIDC; /api calls go through the dev-server proxy (proxy.conf.json).
 */
export const environment = {
  production: false,
  oidcIssuer: 'http://localhost:8080',
  // SPEC-0006 BR10: the product version shown in the Perfil menu comes from build config, never
  // hardcoded in a component. Source of truth is backend/pom.xml <version> (DECISIONS-BASELINE
  // §0015); /release keeps this string in lockstep with the backend bump.
  appVersion: '0.5.0',
};
