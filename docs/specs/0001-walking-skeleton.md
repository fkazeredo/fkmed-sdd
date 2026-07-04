# 0001 - Walking Skeleton — "Meu Plano"

**Status:** Approved

## Goal

FKMed runs end-to-end with one thin slice of real value: an authenticated beneficiary opens
the portal and sees the **"Meu Plano"** screen with their real plan and family data served
by the backend from the database. This slice proves the whole architecture path
(migration → domain → API → screen), the auth seam, pt-BR i18n, every quality gate and CI —
before any feature module is built.

## Scope

- Flyway `V1` migration: `plan` and `beneficiary` tables + canonical seed (§Persistence).
- Public health endpoint and version endpoint (build info); all other API routes secured.
- Embedded Spring Authorization Server with a **dev-only seeded login** linked to MARIA's
  beneficiary record (explicit stub for SPEC-0002, per `simulation-and-mocking.md`).
- Angular shell: top bar, main navigation placeholder, login redirect, **Meu Plano** screen.
- Docker Compose dev stack; all gates of `docs/BOOTSTRAP.md` §2 wired; the 5 CI workflows.

## Business Context

Health-plan operators pre-load contracts and beneficiaries; every portal journey builds on
that data. The canonical family (MARIA, titular; PEDRO, dependent) and their plan are the
reference mass used by the acceptance criteria of the whole spec suite, so they are seeded
first, and the first visible value is "see my plan".

## Business Rules

- **BR1** — `GET /api/system/health` MUST be publicly accessible and report application and
  database status.
- **BR2** — `GET /api/system/version` MUST expose the application version and git commit,
  sourced from build configuration (never hardcoded).
- **BR3** — Every other `/api/**` route MUST require an authenticated user; unauthenticated
  calls MUST receive `401` without leaking route existence details.
- **BR4** — The seed MUST load the canonical plan: name
  `PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP`, ANS registration `326305`, coverage
  `Estadual (RJ)`, copay enabled, **reimbursement enabled**, additive
  `Urg/emerg Nacional Hr — Assistência`.
- **BR5** — The seed MUST load beneficiaries **MARIA CLARA SOUZA LIMA** (titular, card
  `001234567`, CNS `700000000000001`, fictitious valid CPF, birth 1988-03-12) and
  **PEDRO SOUZA LIMA** (dependent of MARIA, card `001234575`, birth date making him 19
  years old at seed date), both linked to the plan of BR4.
- **BR6** — The **Meu Plano** screen MUST display plan data (name, ANS registration,
  coverage, copay flag, reimbursement flag, additives) and the family members (full name,
  role, card number) of the logged-in beneficiary, fetched from the API — no data
  hardcoded in the frontend.
- **BR7** — All UI text MUST come from the pt-BR i18n bundle (product locale); no hardcoded
  user-facing strings.
- **BR8** — The dev login MUST be an explicit, traceable dev-profile-only seam (dev user
  bound to MARIA's beneficiary record), disabled outside the dev profile and replaced by
  SPEC-0002's real account journeys.

## Input/Output Examples

- `GET /api/system/health` (no auth) → `200 {"status":"UP"}`.
- `GET /api/plan/my-plan` (no auth) → `401` (error case).
- `GET /api/plan/my-plan` (authenticated as MARIA) → `200`:
  `{"plan":{"name":"PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP","ansRegistration":"326305",
  "coverage":"ESTADUAL","copay":true,"reimbursement":true,
  "additives":["Urg/emerg Nacional Hr — Assistência"]},
  "members":[{"fullName":"MARIA CLARA SOUZA LIMA","role":"TITULAR","cardNumber":"001234567"},
  {"fullName":"PEDRO SOUZA LIMA","role":"DEPENDENT","cardNumber":"001234575"}]}`.

## API Contracts

| Method | Path | Auth | Response |
|---|---|---|---|
| GET | `/api/system/health` | public | `200` status UP/DOWN |
| GET | `/api/system/version` | public | `200` `{version, commit}` |
| GET | `/api/plan/my-plan` | required | `200` plan + family members · `401` |

DTO shapes are finalized at implementation; the committed OpenAPI snapshot governs.

## Events

Not applicable.

## Persistence Changes

`V1__baseline_plan_and_beneficiary.sql`: table `plan` (id, name, ans_registration,
coverage, copay, reimbursement, additives), table `beneficiary` (id, plan_id, full_name,
cpf, cns, card_number unique, birth_date, role `TITULAR|DEPENDENT`, titular_id nullable,
active) + seed of BR4/BR5. Reference values that will grow (coverage levels) stay simple
columns here; registry tables arrive with the specs that manage them (baseline §0019).

## Validation Rules

Card number: 9 numeric digits, unique. CNS: 15 digits. CPF: 11 digits, valid check digits.
Birth date: valid, in the past.

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Unauthenticated API call | *(framework standard body)* | 401 |
| Authenticated user without beneficiary/plan link | `plan.not-found` | 404 |

## Observability Requirements

Structured startup log; access log excluding health checks; login events logged;
`/api/system/version` feeds deploy verification. No personal data in logs (CPF/CNS masked
by the logging layer).

## Tests Required

- **Domain/unit:** beneficiary/plan invariants (card uniqueness, role linkage).
- **Integration (Testcontainers):** V1 migrates + seed present; `my-plan` returns seeded data.
- **API contract:** OpenAPI snapshot committed and drift-tested.
- **Frontend unit:** shell renders; Meu Plano maps API payload; i18n bundle completeness.
- **E2E (Playwright):** dev login → Meu Plano shows plan name, ANS `326305` and both members.
- **Gates:** full ArchUnit suite with teeth tests, Modulith verify + diagram snapshot,
  Spotless/Checkstyle, JaCoCo floors, i18n and HTTP-mapping completeness.

## Acceptance Criteria

- **AC1** (BR1) — Given the stack is up, when `GET /api/system/health` without auth, then
  `200` with status UP.
- **AC2** (BR3) — Given no authentication, when `GET /api/plan/my-plan`, then `401`.
- **AC3** (BR4, BR5, BR6) — Given the dev login as MARIA, when opening **Meu Plano**, then
  the plan name, ANS `326305`, coverage `Estadual`, the additive, and both family members
  with card numbers are displayed.
- **AC4** (BR2) — Given the app is built, when `GET /api/system/version`, then version and
  commit match the build (not hardcoded).
- **AC5** (BR7) — Given the i18n completeness test runs, then every visible UI string of
  this slice resolves from the pt-BR bundle.

## Open Questions

- ~~**OQ1** — Confirm the walking-skeleton journey is "Meu Plano"~~ — **answered by the
  owner (2026-07-04): confirmed**; the journey is BR6 as written.

## Out of Scope

Real account creation/login/recovery (SPEC-0002); beneficiary selector and authorization
matrix (SPEC-0003); notifications (SPEC-0004); any feature module; production hardening
beyond the fail-fast dev-defaults validator required by the baseline.
