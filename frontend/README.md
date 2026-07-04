# FKMed — Frontend

Angular 22 (standalone, zoneless, signals) + PrimeNG (Aura) + Tailwind 4 + ngx-translate
(pt-BR, in-memory bundle) + angular-oauth2-oidc (Authorization Code + PKCE against the
embedded Authorization Server). Architecture rules: `docs/architecture/frontend-angular.md`.

## Commands (the real gates)

```bash
npm ci                 # install (see .npmrc — legacy-peer-deps rationale)
npm start              # ng serve on :4200, /api proxied to the backend (see Dev proxy)
npm run lint           # ESLint (gate)
npm test               # Vitest, single run, coverage floors 70/75/49/55 (gate)
npm run build          # production build (gate)
```

### E2E (Playwright — isolated stack ONLY)

The E2E suite never touches the dev database. It targets the throwaway compose stack
(`compose.e2e.yaml`: tmpfs Postgres, backend + nginx proxy on **:4201**):

```bash
# from the repository root
docker compose -f compose.e2e.yaml up -d --build --wait
cd frontend
npx playwright install chromium   # first time only
npm run e2e
docker compose -f compose.e2e.yaml down -v
```

Journey covered: dev login (`maria`/`dev12345`, SPEC-0002 seam) → OIDC Code+PKCE →
**Meu Plano** with the seeded plan and family (SPEC-0001 AC3), plus health-through-proxy
and 401 contract checks.

## Dev proxy (`proxy.conf.json`)

`ng serve` proxies `/api` to **`http://127.0.0.1:8080`** — deliberately the IPv4 literal,
not `localhost` (review finding I3): on Windows, Node resolves `localhost` to `::1` first,
and an unrelated listener on the IPv6 loopback port silently answers instead of the FKMed
backend. OIDC is NOT proxied in dev: the SPA does a full-page redirect to the backend origin
(`environment.oidcIssuer` = `http://localhost:8080`), and the token XHR is allowed by the
backend's dev CORS config.

No automated test exercises this file: the E2E suite goes through the nginx proxy of the
isolated stack (never `ng serve`), so the dev proxy path is only exercised by the manual dev
environment (`/dev-env` smoke). That is the stated reason there is no regression test for it.

## Structure

```txt
src/app
  core/    auth (OIDC config, guard, AuthService)  i18n (pt-BR bundle + completeness spec)
           layout (shell: topbar + nav)
  features/my-plan  (PlanApi + Meu Plano screen: loading/error/success states)
e2e/       Playwright journey (isolated stack, :4201)
```

Every user-facing string lives in `src/app/core/i18n/translations.ts`; the completeness spec
renders the slice's screens and fails on any missing key (SPEC-0001 AC5).
