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
| Dev login seam (SPEC-0001 BR8, replaced by SPEC-0002) | `maria` / `dev12345` | `DevLoginConfig` (dev profile only) | `ProdReadinessValidator` (refuses the dev profile/seam) |
| Postgres dev password | `fkmed` | `.env.example`, compose dev/E2E | `ProdReadinessValidator` (refuses the dev DB password) |
| Grafana dev admin | `admin` / `admin` | `.env.example`, compose dev | `compose.prod.yaml`: `GRAFANA_ADMIN_*` are mandatory `:?` variables (the app validator never sees Grafana) |

## Production posture

- TLS terminates at the nginx proxy (`compose.prod.yaml`); Postgres has no public port;
  Grafana binds to loopback; `/actuator/*` and `/v3/api-docs/**` are never routed by the
  proxy (internal network only — the OpenAPI document is the committed public contract
  `docs/api/openapi.json`, so exposing it app-level leaks nothing).
- The OIDC signing key is a persisted PKCS#8 RSA key injected via `OIDC_JWK_PRIVATE_KEY`
  (DECISIONS-BASELINE §0020) — never generated ephemeral in prod.
