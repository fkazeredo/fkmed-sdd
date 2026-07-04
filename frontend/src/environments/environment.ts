/**
 * Dev environment: the SPA runs on ng serve (:4200) and talks to the backend origin (:8080)
 * for OIDC; /api calls go through the dev-server proxy (proxy.conf.json).
 */
export const environment = {
  production: false,
  oidcIssuer: 'http://localhost:8080',
};
