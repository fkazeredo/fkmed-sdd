# Build, Delivery, Infrastructure, Flags and Audit

> Read when: touching build config, dependencies, Git workflow, CI/CD, Docker/deploy,
> feature flags or audit fields.

## Build and versions

Maven is the default for Java/Spring Boot (wrapper only: `backend/mvnw`); never migrate
Maven<->Gradle without explicit instruction and an ADR. Java/Spring Boot/Angular/Node
versions prioritize stability and LTS; no experimental/milestone/RC/snapshot dependencies in
production. Dependencies are chosen conservatively: standard capabilities first, mature
libraries, acceptable licenses; risky dependencies isolated; architecturally significant
dependencies documented in ADRs. Do not add libraries for trivial problems.

Final toolchain: Java 21 · Spring Boot 4.1.0 · Maven wrapper 3.9.x · Node/npm 11.7.0 ·
Angular CLI 22.0.4. Exact library versions live in `backend/pom.xml` and
`frontend/package.json` (summarized in `backend.md` and `frontend-angular.md`).

## Git

Pragmatic Git Flow (`main`, `develop`, `feature/*`, `bugfix/*`, `release/*`, `hotfix/*`) and
Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`).

**Branch protection & PR-only (DECISIONS-BASELINE §0023).** `main` and `develop` are **protected**: **no direct
push**, **no direct merge** — they change **only via a reviewed Pull Request**. Ruleset (both
branches): require a PR, **≥1 review + CODEOWNERS review**, dismiss stale approvals, conversation
resolution, **required status checks** (Backend verify · Mutation/PIT · Flyway validate · Frontend
lint/test/build · Playwright E2E · CodeQL ×2 · **Gitleaks**), branch up to date, **linear history**,
no force-push, no deletions, **include administrators**. Enable GitHub **Secret Scanning + Push
Protection** and Dependabot. PRs target `develop`; `main` is updated only via a **release PR**
(`develop → main`), from which a human cuts the tag. **AI agents never merge to protected branches,
never publish releases, and never force-push** (`.claude/settings.json`); on a green slice they push
the feature branch and open the PR to `develop`, and may tag only on explicit request. See
`CONTRIBUTING.md` and `SECURITY.md`.

Semantic Versioning (`MAJOR.MINOR.PATCH`) is the official policy — see **DECISIONS-BASELINE §0015** for the per-digit
criteria, reset rules, the `0.y.z` initial-development phase and the mapping to ROADMAP phases /
Conventional Commits. The version's source of truth is `backend/pom.xml`; the release tag is cut from
`main` **via the release PR**. Docs-only slices do **not** bump the version (standing precedent).

Generated files are never edited manually — modify the generation source (OpenAPI snapshot
via `-Dopenapi.snapshot.write=true`, module diagram via `-Dmodulith.diagram.write=true`).

## CI/CD (real pipelines — `.github/workflows/`)

| Workflow | What it does |
|---|---|
| `ci.yml` | backend `./mvnw verify` (tests + ArchUnit/Modulith + Spotless/Checkstyle + JaCoCo floors + snapshot gates), frontend lint/test+coverage/build, `npm audit --audit-level=critical` gate, PIT mutation job, artifacts (JaCoCo always, surefire on failure) |
| `e2e.yml` | Playwright suite against the isolated `compose.e2e.yaml` stack |
| `codeql.yml` | CodeQL static analysis (java manual build + ts), weekly + on PR |
| `gitleaks.yml` | **secret scanning** (blocking) on push/PR, full history, allowlisting the enumerated dev defaults (`.gitleaks.toml`) |
| `docker-publish.yml` | backend/frontend images to **GHCR** tagged per release |

Dependabot watches Maven, npm and GitHub Actions. Failed tests, broken builds, invalid
migrations or broken contracts block merge/deploy. Concurrency groups cancel superseded runs.

`ci.yml` and `e2e.yml` run on **pull requests and protected/release branches only** — a push
to a `feature/**`/`bugfix/**` branch does not trigger them (the PR run would duplicate it;
dev-time validation is the local gates, and the E2E suite must be green locally before the
push/PR — owner order, Phase-4 lesson). `workflow_dispatch` covers the rare mid-slice manual
run.

## Local development and configuration

Reproducible local env: `docker-compose.yml` (app db + observability; optional `emulators`
profile with WireMock for the NFS-e integration), `compose.e2e.yaml` (isolated E2E stack,
tmpfs Postgres, frontend :4201), `.env.example` provided. Configuration is externalized,
typed and validated at startup; secrets never committed (env vars; the `prod` profile
fail-fasts on dev defaults — `ProdReadinessValidator`).

## Deployment and IaC

Production ships as **`compose.prod.yaml`** (DECISIONS-BASELINE §0021): nginx reverse proxy
terminating TLS with SPA + API + AS under one origin (HSTS, conservative CSP), Postgres
without a public port, observability on the internal network, Grafana on loopback, images
from GHCR by tag or built locally. Backup/DR per **DECISIONS-BASELINE §0021**: daily `pg_dump -Fc` + vault
tar (`infra/backup/backup.sh`), 30d local retention + offsite copies, RPO 24h / RTO 4h,
quarterly restore drill.

Business logic **MUST NOT** depend on cloud SDKs or deployment-specific details — abstract
storage, messaging and notifications when variation is possible. Never create
Terraform/Helm/K8s files unless the project context requires it.

## Feature flags

Flags reduce delivery risk; they **MUST NOT** become permanent hidden complexity. Every flag
has: name, purpose, owner, scope, default, removal condition, and tests for both states when
business logic changes. Most flags are temporary (the OverrideNudge flag was removed when the
real model shipped in 20c); long-lived flags only for real product configuration (e.g.
`BILLING_TAX_REGIME_CONFIRMED`, pending the accountant's decision).

## Audit and history

Auditability proportional to business relevance. Relevant entities track createdAt,
updatedAt, createdBy, updatedBy. Important business actions audited in business language
(`ManualOverridePerformed`, `RateFrozen`, `PeriodClosed`). Critical domains **MAY** need
richer history: state transitions, decisions, manual overrides, DSS insights and the human
decision on each, operational timeline.
