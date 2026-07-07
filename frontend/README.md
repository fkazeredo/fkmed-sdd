# FKMed - Frontend

Angular 22 (standalone, zoneless, signals) + PrimeNG (Aura) + Tailwind 4 + ngx-translate
(pt-BR, in-memory bundle) + angular-oauth2-oidc (Authorization Code + PKCE against the
embedded Authorization Server). Architecture rules: `docs/architecture/frontend-angular.md`.

## Commands

```bash
npm ci
npm start
npm run lint
npm test
npm run build
```

`npm start` runs `ng serve` on `:4200`, with `/api` proxied to the backend.

## E2E

The E2E suite never touches the dev database. It targets the throwaway compose stack
(`compose.e2e.yaml`: tmpfs Postgres, backend + nginx proxy on `:4201`):

```bash
# from the repository root
cd frontend && npm run e2e:up
cd frontend && npx playwright install chromium
cd frontend && npm run e2e
cd frontend && npm run e2e:down
```

Journeys use the DB-backed dev account `maria@fkmed.local` / `maria12345` and exercise the
seeded beneficiary flows through OIDC Code + PKCE. Older `maria` / `dev12345` dev-login
seams were retired in SPEC-0002.

## Dev proxy (`proxy.conf.json`)

`ng serve` proxies `/api` to `http://127.0.0.1:8080` - deliberately the IPv4 literal, not
`localhost`: on Windows, Node may resolve `localhost` to `::1` first, and an unrelated
listener on the IPv6 loopback port can answer instead of the FKMed backend.

OIDC is not proxied in dev: the SPA does a full-page redirect to the backend origin
(`environment.oidcIssuer` = `http://localhost:8080`), and the token XHR is allowed by the
backend's dev CORS config.

## Structure

```text
src/app
  core/       auth, i18n, layout, beneficiary context
  features/   product features by slice
e2e/          Playwright journeys against the isolated stack
```

Every user-facing string lives in `src/app/core/i18n/translations.ts`; the completeness spec
renders the slice's screens and fails on missing keys.
