# Security Policy

## Reporting a vulnerability

Please report vulnerabilities **privately** via GitHub Security Advisories
(*Security → Report a vulnerability*) or by e-mail to the repository owner
(@fkazeredo). Do **not** open public issues for security problems. You will get an
acknowledgement within 72 hours.

## Secrets policy (DECISIONS-BASELINE §0023)

- **No secret is ever committed**: `.gitignore` blocks `.env*` (except `*.example`) and all
  key/certificate material; **gitleaks** runs as a blocking CI check and as a pre-commit hook
  (`.pre-commit-config.yaml`); GitHub secret scanning + push protection should be enabled by
  the owner.
- Production secrets live only in `.env.prod` on the deployment host and in GitHub Actions
  secrets.

## Enumerated dev-only defaults

The ONLY credentials in this repository are the following **fictitious dev defaults**,
allowlisted in `.gitleaks.toml`. Each row lists what blocks it in production:

| Credential | Value | Where | Blocked in prod by |
|---|---|---|---|
| MARIA dev account (SPEC-0002, replaces the retired in-memory seam) | `maria@fkmed.local` / `maria12345` | Flyway `V3` seed (all envs) | `ProdReadinessValidator` (refuses to boot when the seeded account + dev password are present) |
| Disposable account-security E2E account (SPEC-0003 débito B; keeps the E2E off MARIA) | `seguranca-e2e@fkmed.local` / `seguranca12345` | Flyway `V7` seed (all envs) | `ProdReadinessValidator` (refuses to boot when the seeded account + dev password are present) |
| Disposable profile E2E account (SPEC-0006 Phase 2; keeps the profile E2E off MARIA) | `perfil-e2e@fkmed.local` / `perfilE2e12345` | Flyway `V13` seed (all envs) | `ProdReadinessValidator` (refuses to boot when the seeded account + dev password are present) |
| Disposable terms-interception E2E account (SPEC-0006 Phase 2; drives the Terms interception BR8) | `termos-e2e@fkmed.local` / `termosE2e12345` | Flyway `V13` seed (all envs) | `ProdReadinessValidator` (refuses to boot when the seeded account + dev password are present) |
| Postgres dev password | `fkmed` | `.env.example`, compose dev/E2E | `ProdReadinessValidator` (refuses the dev DB password) |
| Grafana dev admin | `admin` / `admin` | `.env.example`, compose dev | `compose.prod.yaml`: `GRAFANA_ADMIN_*` are mandatory `:?` variables (the app validator never sees Grafana) |

> The former `DevLoginConfig` in-memory seam (`maria` / `dev12345`) was **retired** in the
> SPEC-0002 slice; real DB-backed accounts replace it. The registration-token HMAC secret
> (`REGISTRATION_TOKEN_SECRET`, DL-0001) is empty in dev (ephemeral) and **mandatory in prod**
> (also refused empty by `ProdReadinessValidator`).

## Production posture

- TLS terminates at the nginx proxy (`compose.prod.yaml`); Postgres has no public port;
  Grafana binds to loopback; `/actuator/*` and `/v3/api-docs/**` are never routed by the
  proxy (internal network only — the OpenAPI document is the committed public contract
  `docs/api/openapi.json`, so exposing it app-level leaks nothing).
- The OIDC signing key is a persisted PKCS#8 RSA key injected via `OIDC_JWK_PRIVATE_KEY`
  (DECISIONS-BASELINE §0020) — never generated ephemeral in prod.
