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
allowlisted in `.gitleaks.toml` and **refused in production** by the fail-fast
`ProdReadinessValidator` (`prod` profile will not boot with any of them):

| Credential | Value | Where |
|---|---|---|
| Dev login seam (SPEC-0001 BR8, replaced by SPEC-0002) | `maria` / `dev12345` | `DevLoginConfig` (dev profile only) |
| Postgres dev password | `fkmed` | `.env.example`, compose dev/E2E |
| Grafana dev admin | `admin` / `admin` | `.env.example`, compose dev |

## Production posture

- TLS terminates at the nginx proxy (`compose.prod.yaml`); Postgres has no public port;
  Grafana binds to loopback; `/actuator/*` is never routed by the proxy (internal network
  only).
- The OIDC signing key is a persisted PKCS#8 RSA key injected via `OIDC_JWK_PRIVATE_KEY`
  (DECISIONS-BASELINE §0020) — never generated ephemeral in prod.
