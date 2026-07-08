# Build, Delivery, Infrastructure, Flags and Audit

> Read when: touching build config, dependencies, Git workflow, CI/CD, Docker/deploy, feature
> flags, production posture or audit fields.

## Build and Versions

Backend uses Maven through `backend/mvnw`; never use a system Maven in project commands.
Frontend uses npm scripts from `frontend/package.json`.

Current stack highlights:

- Java 21, Spring Boot 4.1.0, Spring Modulith 2.1.0;
- Node/npm as declared by `frontend/package.json` (`npm@11.7.0`);
- Angular 22, PrimeNG 21, Tailwind 4;
- PostgreSQL 16 and Flyway migrations.

Do not add experimental/RC/snapshot dependencies to production code. Architecturally significant
dependencies require an ADR.

## Git and PR Flow

Use pragmatic Git Flow:

- work on `feature/*`, `bugfix/*`, `hotfix/*` or release branches;
- PRs target `develop`;
- `main` changes through release PRs;
- agents never merge protected branches, force-push, tag or publish releases without explicit owner
  instruction.

Conventional commits are expected for local commits/PRs. The version source of truth is
`backend/pom.xml`; release tags are owner actions.

## CI/CD

Real workflows live in `.github/workflows/`:

- backend verify, coverage, architecture and snapshot gates;
- frontend lint/test/build and critical audit gate;
- Playwright E2E against isolated stack;
- CodeQL;
- Gitleaks;
- Docker image publishing to GHCR.

Feature-branch pushes do not need duplicate CI runs when the PR will run the protected checks.
Local gates are the development proof before opening the PR.

## Local Development

Typical local flow:

```bash
docker compose up -d
cd backend && ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
```

E2E uses an isolated stack:

```bash
cd frontend
npm run e2e:up
npm run e2e
npm run e2e:down
```

## Deployment

Production is represented by `compose.prod.yaml`: TLS-terminating nginx proxy, one public origin for
SPA/API/OIDC, Postgres without public port and Grafana bound to loopback. The proxy owns HSTS/CSP
headers and request-body limits.

Backup/DR remains a baseline target (daily `pg_dump`, document vault copy, retention and restore
drills per DECISIONS-BASELINE §0021), but no committed `infra/backup/backup.sh` implementation exists
yet. Do not claim it is implemented until a future production-readiness slice adds and tests it.

Business logic must not depend on cloud SDKs or deployment-specific storage. Introduce abstractions
when variation becomes real.

## Feature Flags

Flags must not become permanent hidden complexity. Each flag needs purpose, scope, default, owner,
tests for relevant states and removal condition.

`app.sim.enabled` is a dev/E2E flag and is refused in production by `ProdReadinessValidator`.

## Audit

Auditability is proportional to business impact. Security/account events, family-scope access to
dependent-sensitive data, clinical document access, support requests, guide/token transitions,
finance operations and reimbursement transitions must be recorded when the spec requires it.

Audit details must already be masked before they are stored.
