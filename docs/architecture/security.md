# Security, User Context, Privacy and Multi-Tenancy

> Read when: touching authentication, authorization, user context, sensitive data, LGPD concerns
> or tenant boundaries.

## Authentication Model

FKMed uses the embedded Spring Authorization Server from DECISIONS-BASELINE §0018/§0020.

- The SPA authenticates with Authorization Code + PKCE.
- The API validates RS256 JWTs as an OAuth2 Resource Server.
- Form login is backed by `user_account`, lockout and password policy.
- Authorization-server state and form-login sessions are JDBC-backed for multi-instance readiness.
- Signing keys are persisted through `OIDC_JWK_PRIVATE_KEY` in production.

Dev-only credentials are fictitious, enumerated in `SECURITY.md`, allowlisted in `.gitleaks.toml`
and refused by `ProdReadinessValidator` under the `prod` profile.

## Endpoint Authorization

Current POC behavior:

- `/api/system/health`, `/api/system/version` and first-access/recovery/verification endpoints are
  public by explicit `SecurityConfig` rules;
- the remaining `/api/**` endpoints require an authenticated JWT;
- operator-simulation endpoints are additionally guarded by the `app.sim.enabled` flag and the
  operator-sim allowlist/role path;
- business authorization is enforced inside services/facades, especially family-scope beneficiary
  access through `BeneficiaryAccess`.

Future production hardening: a complete route-role authorization matrix with a default-deny
completeness test is desirable before introducing real operator/backoffice roles. It is not yet
implemented and must not be cited as current behavior.

## Business Authorization

The backend is the final authority. The frontend may hide menu items or guide the user, but cannot
be the enforcement layer.

Use domain/application policies when authorization depends on business state, ownership, workflow
or plan entitlement. Keep simple checks in the service when they are local and readable. Important
authorization defects need integration tests and, when reachable in UI, frontend/E2E coverage.

## User Context

Application and controller code should use `com.fkmed.infra.security.UserContextProvider` instead
of reading `SecurityContextHolder` directly. The adapter resolves the current username and beneficiary
card server-side; sensitive beneficiary identifiers are not trusted from the frontend.

## Secrets and Production Posture

- Secrets come from environment variables or deployment secrets, never from committed `.env` files.
- `.gitignore`, `.gitleaks.toml` and CI secret scanning protect against committed secrets.
- `ProdReadinessValidator` refuses the prod profile when dev defaults are present, including seeded
  dev accounts, operator-sim configuration, dev DB password, HTTP issuer, localhost origins or empty
  registration/OIDC key material.
- TLS terminates at the production nginx proxy; Postgres is not publicly exposed; Grafana is bound
  to loopback.
- Production proxy config does not route `/actuator/*` or `/v3/api-docs/**` to the public origin.
- Production uploads use a private encrypted S3 bucket through `FileStorage` (ADR-0023). The SDK
  default credential chain is used, IAM roles are preferred, and `StorageProductionValidator`
  refuses prod without the S3 backend, bucket and region.

## Privacy and LGPD

Current implemented privacy posture:

- CPF, CNS and bank data are masked outside explicitly allowed contexts;
- audit details store masked values;
- notification content avoids full sensitive data;
- access logs avoid query/body logging;
- authentication event logs mask e-mail hints;
- a correlation ID is logged for request tracing without using personal data as a log label.

Future production privacy work still needed before a real launch: full data-subject request process,
retention schedules by data category, export/correction/deletion workflows, incident-response
runbook and operator/backoffice access controls.

Always apply low-cost hygiene:

- never log passwords, tokens, credentials, secrets or raw bank data;
- avoid personal data in URLs and log labels;
- return neutral auth errors to prevent account enumeration;
- avoid exposing internal exception details in API errors;
- keep upload validation and authorization server-side.
- never derive filesystem paths or S3 keys from client filenames.

## Multi-Tenancy

The system is single-tenant per build (ADR-0003). If a real tenant concept arrives, it becomes an
architectural boundary across requests, security context, data access, cache keys, logs, metrics,
events, jobs and integrations. Tenant leakage is a critical bug and requires an ADR/spec before
implementation.
