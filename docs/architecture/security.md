# Security, User Context, Privacy and Multi-Tenancy

> Read when: touching authentication, authorization, user context, sensitive data, LGPD
> concerns, or anything tenant-related.

## Security baseline (final model)

Spring Security is the default for authentication, authorization, endpoint protection,
filters, security context, CORS and IdP integration. Do not reinvent auth mechanisms.

The model (DECISIONS-BASELINE §0018/§0020):

- **IdP self-hosted and embedded:** the app runs a **Spring Authorization Server** inside the
  same process (no Keycloak, no extra container) serving `/oauth2/authorize|token|jwks`,
  `/.well-known/openid-configuration`, `/userinfo` and `/login` (form login with DB-backed
  lockout — 5 failures → 15 min — and a minimal password policy).
- **The API is an OAuth2 Resource Server** validating RS256 JWTs; the SPA authenticates with
  Authorization Code + **PKCE** (public client `product-web`); silent refresh is same-origin
  in production (TLS reverse proxy).
- **Multi-instance ready:** signing key persisted (`OIDC_JWK_PRIVATE_KEY`, stable `kid`), AS
  client/authorization state and form-login session in JDBC (DECISIONS-BASELINE §0020) — no sticky sessions.
- **Dev-only accounts are enumerated, fictitious and blocked in production:** the canonical
  list lives in `SECURITY.md` and Flyway seed comments (for example
  `maria@fkmed.local` / `maria12345` and `operador-sim@fkmed.local` /
  `operador12345`). Do not invent extra seeded users in docs or tests.

## Endpoint authorization — default-deny matrix

Authorization is centralized in the **`ApiAuthorizationMatrix`**: an ordered
route×role registry consumed by `SecurityConfig`, with **default-deny** for any unmapped
write (`POST/PUT/PATCH/DELETE /api/**`). A build-time completeness test fails when a write
endpoint is missing from the matrix or a rule goes stale. Machine-to-machine access is
narrowed to the two HMAC webhooks (signature + timestamp anti-replay window of 300s,
anti-replay window). Sensitive reads are role-gated too (People/Ponto → IT for LGPD; vault content
excludes VIEWER).

## Business authorization

Spring Security handles general access; business authorization depends on domain state,
ownership, workflow or permissions. Simple checks **MAY** stay in the Application Service;
complex/reusable/critical rules go into policy classes (e.g. the Compliance close-veto
guard). The backend is the final authority — the frontend only mirrors permissions (menu
gating by role), never enforces them.

## Current user context

Application Services **SHOULD NOT** access `SecurityContextHolder` directly across the
codebase. Use the centralized `UserContextProvider` port (`com.example.product.infra.security`)
exposing userId, username and roles; the `SecurityContextUserProvider` adapter reads the JWT.

## Secrets and production posture

- Secrets come from the environment (`.env.prod`, never committed); the **`ProdReadinessValidator`**
  refuses to boot the `prod` profile with any dev default (DB password, OIDC issuer over
  http, empty webhook/metrics secrets, unconfirmed tax regime, localhost origins — 9 checks,

- Platform-held credentials (e-CNPJ material) are encrypted at rest with **AES-GCM** under a
  32-byte master key (`PLATFORM_SECRET_KEY`).
- Uploads to the document vault are validated by **magic bytes** and stored under an opaque
  `fileRef` UUID that is never exposed.
- **No secret is ever committed** (DECISIONS-BASELINE §0023): `.gitignore` blocks `.env*` (except `*.example`) and
  all key/cert material; **gitleaks** runs as a blocking CI check + optional pre-commit hook
  (`.gitleaks.toml`); enable GitHub **secret scanning + push protection**. The only in-repo
  credentials are the **enumerated dev-only defaults** (allowlisted, blocked in prod by
  `ProdReadinessValidator`) — see `SECURITY.md`.

## Branch protection & change control (DECISIONS-BASELINE §0023)

`main` and `develop` are **protected**: changes land **only via a reviewed Pull Request** (≥1 review
+ CODEOWNERS + required checks incl. Gitleaks + linear history, no force-push, no direct push,
include admins). AI agents push feature branches and open PRs but **never merge to protected
branches, never publish releases, never force-push** (`.claude/settings.json`). See `CONTRIBUTING.md`.

## Privacy and LGPD

Compliance is driven by project context and regulation — the system already practices:
consent per purpose (Marketing), personal-data masking in logs, LGPD erasure with tombstone,
and role-gated access to people/point data. Always apply the low-cost hygiene:

- never log passwords, tokens, credentials, secrets;
- never expose sensitive internal data in API errors;
- never send secrets to the frontend;
- avoid unnecessary personal data in logs; mask sensitive values;
- do not store sensitive data without reason.

## Multi-tenancy

The system is **single-tenant with extension seams** (DECISIONS-BASELINE §0003). If a real tenant concept
ever arrives, it becomes an architectural boundary: tenant context propagated consistently
through requests, security context, services, repositories, queries, cache keys, logs,
metrics, audit, events, jobs and integrations. Data leakage between tenants is a critical
bug. The isolation strategy **MUST** be explicit and documented in an ADR.
