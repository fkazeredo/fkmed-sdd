# BOOTSTRAP — from an empty repository to a green walking skeleton

This is the build recipe Claude Code follows to turn this foundation into a running project
with the same architecture, code design and quality bar as the parent project. Work through
it top to bottom. **Ask the owner whenever a decision below is marked `OWNER:` — never
assume** (CLAUDE.md invariant 3).

## 0. Parameterize the foundation

Collect from the owner, then find/replace across the repo:

| Placeholder | Meaning | Example |
|---|---|---|
| `com.example.product` | Java base package | `com.clinicsoft.core` |
| Product name | OpenAPI title, frontend branding, README | "ClinicSoft" |
| Docker/GHCR image names | CI publishing + prod compose | `ghcr.io/org/clinicsoft-backend` |

`OWNER:` decisions to record now:
- **Product language(s):** UI i18n locale(s) and user-manual language(s). Default: en-US
  only. If more than one, every localized artifact moves in the same slice (CLAUDE.md
  §Language policy).
- **Domain summary:** actors, first value journey — input for SPEC-0001.

## 1. Stack manifest (versions proven together)

| Layer | Technology | Version |
|---|---|---|
| Backend language | Java (LTS) | 21 |
| Framework | Spring Boot | 4.1.x |
| Modularity | Spring Modulith | 2.1.x |
| Persistence | Spring Data JPA + PostgreSQL | Postgres 16 |
| Migrations | Flyway (modular starter) | Boot BOM |
| Security | Spring Security + OAuth2 Resource Server + **embedded** Spring Authorization Server | Boot BOM |
| Session/HA | Spring Session JDBC + persisted JWK | Boot BOM |
| API docs | springdoc-openapi | 3.x |
| Frontend | Angular (standalone, zoneless, signals) | 22.x |
| UI kit | PrimeNG + primeicons + Angular CDK | current majors |
| Utility CSS | Tailwind CSS | 4.x |
| Frontend i18n | @ngx-translate/core (in-memory loader) | 18+ |
| Frontend auth | angular-oauth2-oidc (Code + PKCE) | 20+ |
| Backend tests | JUnit 5 · Testcontainers · ArchUnit · jqwik · PIT | current |
| Frontend tests | Vitest (+coverage-v8) · Playwright | current |
| Quality | Spotless (google-java-format) · Checkstyle · JaCoCo · ESLint · Prettier | current |
| Observability | Micrometer + Prometheus + Loki + Grafana Alloy + Grafana | official images |
| Infra | Docker Compose (dev / E2E / prod with nginx TLS proxy) | — |
| CI/CD | GitHub Actions + Dependabot | — |

## 2. Gates manifest (wire ALL of these into `./mvnw verify` — they define the quality bar)

Every gate below **breaks the build**. Never weaken one to make code pass (invariant 5).

1. **Spotless** — google-java-format; `spotless:apply` to fix.
2. **Checkstyle** — fail on warning.
3. **JaCoCo floors** — instruction ≥ **0.80**, branch ≥ **0.65**.
4. **ArchUnit suite** (each rule ships with a teeth test) — at minimum:
   - layer rules from DECISIONS-BASELINE §0010/0012 (`domain` imports nothing above;
     `infra` never imports `application`);
   - `@ModuleInternal` visibility rule (§0016);
   - Lombok rules: no `@Data`/`@Setter` on entities (§0013);
   - naming: no `*Impl`; constructor injection only.
5. **Spring Modulith** — `ApplicationModules.verify()` (acyclic, boundaries) **+ committed
   module-diagram snapshot** with a drift test; regenerate via
   `-Dmodulith.diagram.write=true`.
6. **OpenAPI contract snapshot** — committed `docs/api/openapi.json` + drift test;
   regenerate via `-Dopenapi.snapshot.write=true`.
7. **i18n completeness test** — every `DomainException.code` has a message in every product
   locale bundle; bundles have full key parity.
8. **HTTP mapping completeness test** — every `DomainException` subclass has an HTTP status
   (§0011).
9. **PIT mutation testing** — separate profile (`-Pmutation`), threshold **60**, scoped to
   money/critical domain packages.
10. **jqwik property tests** — for money/critical arithmetic (properties × ~1000 cases).
11. **build-info + git-commit-id** plugins — feed a `/api/version` endpoint.

Frontend gates: `npm run lint` (ESLint), `npm test` (Vitest; i18n parity spec if
multilingual), `npm run build`. E2E: Playwright against the isolated stack only.

## 3. Infrastructure files to create

- **`docker-compose.yml`** (dev): `db` (postgres:16-alpine, healthcheck) + `app` (built from
  `backend/Dockerfile`, depends_on healthy db) + observability (prometheus, loki, alloy,
  grafana with provisioned datasources/dashboards). Config via `.env` (+ `.env.example`
  committed with safe defaults).
- **`compose.e2e.yaml`**: fully isolated throwaway stack — ephemeral tmpfs Postgres, own
  ports, own project name. E2E never touches the dev database.
- **`compose.prod.yaml`**: nginx TLS proxy in front; Postgres with no public port; app on the
  `prod` profile with a **fail-fast startup validator** that refuses every dev default;
  Grafana on loopback.
- **`backend/Dockerfile`**: multi-stage (temurin-21 JDK build → JRE runtime).
- **CI — 5 GitHub Actions workflows** (job names become required status checks):
  - `ci.yml` — backend verify (ArchUnit/Modulith/Testcontainers) · mutation (PIT) · flyway
    validate on real Postgres · frontend lint+test+build+`npm audit` (fail on critical).
  - `e2e.yml` — Playwright against the isolated compose stack.
  - `codeql.yml` — java-kotlin + javascript-typescript, weekly cron.
  - `gitleaks.yml` — blocking secret scan, full history, `GITHUB_TOKEN` env set for PR events.
  - `docker-publish.yml` — build & push images to GHCR on SemVer tag.
- **Secret hygiene**: `.gitignore` with secret globs (`.env.*` with `!*.example` negations,
  `*.pem *.key *.p12 *.jks` etc.), `.pre-commit-config.yaml` with the gitleaks hook,
  `.gitleaks.toml` (default rules + allowlist of the enumerated dev-only defaults you seed).
- **Governance**: `.github/CODEOWNERS` (owner's handle), `SECURITY.md` (private disclosure),
  `CONTRIBUTING.md` (PR-only flow, gates, branch model), `.github/PULL_REQUEST_TEMPLATE.md`
  (checklist: no secrets, tests, spec/ADR, migration, i18n, target=develop). Then the owner
  enables branch protection + secret scanning on GitHub (only he can).

## 4. Documentation skeleton to create

`docs/README.md` (index) · `docs/specs/README.md` (spec index, starts with 0001) ·
`docs/adr/README.md` (ADR index — note pointing to `docs/DECISIONS-BASELINE.md` as the
inherited baseline) · `docs/decision-log/INDEX.md` (empty, with the ⚠️ highlight-table header)
· `docs/MANUAL.md` skeleton in the product locale(s) · `docs/release-notes/CHANGELOG.md` ·
`docs/ROADMAP.md` + `docs/ROADMAP-STATUS.md` (with the execution-log table header) ·
`README.md` of the product.

## 5. Build sequence (walking skeleton first — no empty architecture)

1. **SPEC-0001** via `/spec`: the walking skeleton — one thin end-to-end slice of real value
   (health endpoint + one domain entity + one migration + one screen + dev login). Owner
   answers the Open Questions.
2. **Skeleton that RUNS**: minimal backend (one module + `domain.error` kernel + health/
   version endpoints + V1 migration + embedded AS with a dev user seeder), minimal frontend
   (shell + login + one screen), compose up → health UP through the frontend proxy.
   **Forbidden** (Rule Zero): empty module trees, fake bounded contexts, placeholder classes.
3. **Gates green**: wire §2 progressively but ALL before the first release; teeth tests prove
   each ArchUnit rule bites.
4. **CI green** on the first PR (the 5 workflows).
5. **Release 0.1.0** via `/release` (pom = source of truth; the owner cuts the tag).
6. From here on: normal slices — `/slice` → build (agent team, see
   `docs/GUIA-TIME-CLAUDE.md`) → `/dod` → PR → owner merges.

## 6. Final verification checklist

- `docker compose up -d` → `GET /api/system/health` = UP, direct AND through the frontend
  proxy (`/dev-env` automates this).
- `cd backend && ./mvnw verify` green, including every gate in §2.
- `cd frontend && npm run lint && npm test && npm run build` green; E2E journey green on the
  isolated stack.
- `gitleaks detect` clean; `.env.example` committed, real `.env` ignored.
- Every ArchUnit rule has a teeth test; coverage floors active; snapshots committed.
- Owner has: branch protection enabled, CI secrets set, CODEOWNERS with real handles.
