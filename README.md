# FKMed — Portal do Beneficiário

Web portal for health-plan beneficiaries: digital card and attendance token, accredited
network search, appointments, telemedicine, authorization guides, contract finances
(invoices, copay, income tax) and the full **reimbursement** journey (request, preview,
tracking, statement). POC with real-system behavior: registered users, persisted data,
protocols, notifications and audit trail.

Built with Spec-Driven Development on the project's own foundation (constitution +
architecture baseline + agent team). This README is the operational entry point; the
knowledge lives in `docs/`.

## Stack

Java 21 · Spring Boot 4.1 · Spring Modulith · PostgreSQL 16 · Flyway · embedded Spring
Authorization Server — Angular 22 (standalone, zoneless, signals) · PrimeNG · Tailwind 4 ·
ngx-translate (pt-BR product locale) — JUnit 5 · Testcontainers · ArchUnit · jqwik · PIT ·
Vitest · Playwright — Docker Compose · GitHub Actions. Full manifest:
[`docs/BOOTSTRAP.md`](docs/BOOTSTRAP.md).

## Repository map

| Path | Content |
|---|---|
| `CLAUDE.md` | The constitution — operating rules loaded on every request |
| `docs/specs/` | Product specs (index + template) — source of truth for behavior |
| `docs/ROADMAP.md` · `docs/ROADMAP-STATUS.md` | Phases (end-to-end deliverables) · execution log |
| `docs/architecture/` | Architecture rule docs (routing map in `CLAUDE.md`) |
| `docs/DECISIONS-BASELINE.md` · `docs/adr/` | Inherited decisions · project ADRs |
| `docs/decision-log/` | Autonomous decisions (DL) under authorized autonomy |
| `docs/MANUAL.md` | User manual (pt-BR, living artifact) |
| `docs/release-notes/CHANGELOG.md` | Consolidated release notes |
| `backend/` · `frontend/` | *(created by roadmap phase 0 — walking skeleton)* |

## How to run

```bash
docker compose up -d                                      # db + app + observability
cd frontend && npm ci && npm start                        # SPA on :4200 (proxy to :8080)
```

Dev login (walking skeleton, SPEC-0002 seam): `maria` / `dev12345`. URLs: app
http://localhost:4200 · API/AS http://localhost:8080 · Grafana http://localhost:3000.

```bash
cd backend && ./mvnw verify                               # build + all quality gates
cd backend && ./mvnw -Pmutation org.pitest:pitest-maven:mutationCoverage  # PIT
cd frontend && npm run lint && npm test && npm run build  # frontend gates
docker compose -f compose.e2e.yaml up -d --build --wait && cd frontend && npm run e2e  # E2E
```

Dev URLs, logins and smoke checks are automated by the `/dev-env` skill.

## Contributing / process

Work happens on `feature/*` branches from `develop`; every slice follows `/slice` →
implementation with tests → `/dod` (gates + manual + changelog) → push + **PR to
`develop`**. Agents never merge, tag or force-push; the owner merges every PR
(`docs/DECISIONS-BASELINE.md` §0023). Secrets never enter the repo (gitleaks in CI +
pre-commit).
