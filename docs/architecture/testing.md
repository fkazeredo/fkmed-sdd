# Testing

> Read when: writing or changing any test, or deciding test strategy for a change.

## Philosophy

Tests protect behavior, prevent regressions, make refactoring safer and document
expectations. Coverage is a signal, not the goal; high coverage with weak assertions is not
quality.

## The real pyramid (final — v0.51.1, all enforced at `./mvnw verify` / CI)

| Level | Tooling | Gate |
|---|---|---|
| Unit (domain rules, VOs, state machines) | JUnit 5 | part of `verify` (617 backend tests total) |
| Property-based (money math, cent distribution) | jqwik 1.9.2 — 8 properties × 1000 cases | part of `verify` |
| Integration (persistence, API, events, security) | Testcontainers (Postgres 16), `TestJwtTokens` | part of `verify` |
| Architecture | ArchUnit (18 rules) + Spring Modulith `verify()` (23 modules, acyclic) | fails the build on violation |
| Contract | OpenAPI snapshot (`docs/api/openapi.json`) + module-diagram drift gates | fails the build on drift |
| Mutation (money-math domain) | PIT 1.17.3, `-Pmutation` profile, own CI job | `mutationThreshold=60` (measured: 68% killed, 89% test strength) |
| Coverage floors | JaCoCo INSTRUCTION ≥ 0.80, BRANCH ≥ 0.65 · Vitest statements 70 / lines 75 / functions 49 / branches 55 | fails the build below the floor |
| Frontend unit | Vitest 4 (jsdom), 331 tests | `npm test` |
| E2E | Playwright 1.61 on the **isolated** stack (`compose.e2e.yaml`: ephemeral tmpfs Postgres, frontend :4201) — never the dev database | `npm run e2e` / CI job |
| Smoke | `/api/system/health` liveness + readiness | part of `verify` / compose healthcheck |

## Backend

Prioritize: domain rules, state transitions, invariants, Application Services, business
exceptions, validation, repositories/queries, transactions, locking, integration boundaries
and ACLs, messaging, API contracts, security-sensitive flows (401/403/409 sad paths),
concurrency (the model: prove the bug red, then fix to green).

Unit tests cover domain/application logic without infrastructure. Integration tests cover
persistence, transactions, APIs, events — use Testcontainers when infrastructure behavior
matters. Architecture tests (ArchUnit / Spring Modulith `verify()`) run in the normal test
suite and **MUST NOT** be weakened to make code pass.

## Regression tests

Every defect found — a shipped bug, a review finding, or anything caught while building —
**MUST** get a regression test that fails before the fix and passes after. If a regression
test is genuinely impossible, explain why in the final response.

**Cover every applicable layer, not just the most convenient one (owner rule).** A
defect usually surfaces at one layer but its root cause and its contract live at several. Add
a regression test at **each layer the defect can reach**, choosing from the pyramid above:

- domain/unit — when an invariant, value object, state machine or calculation was wrong;
- integration (Testcontainers) — when persistence, a transaction, an event, a query or an
  authorization rule was wrong (this is where most backend defects belong);
- API/contract — when a status code, error code or response shape was wrong;
- frontend unit — when a service, guard, form or component behaved wrong;
- E2E — when a critical user journey broke end to end.

One layer is not enough when the defect spans more: e.g. a wrong authorization rule gets both
an integration test (the endpoint returns 403/allows the gate) **and** a frontend test (the
menu/guard reflects it). Skipping an applicable layer is only acceptable with a stated reason.

## Frontend

Protect real behavior: feature/API/state services, guards (`canDeactivate`/auth),
interceptors, validators, error handling, permission behavior, loading/empty/error states,
submit flows, keyboard shortcuts and critical user journeys. E2E for critical flows only.

## Test environments

Right level of realism: don't mock everything blindly; don't boot the whole stack for simple
logic. Mocks/fakes for logic and orchestration; Testcontainers for infrastructure behavior;
the JDK `HttpServer` (no extra dependency) for HTTP-adapter tests; WireMock only in the dev
emulator compose profile. Separate unit, integration and E2E commands. Tests create their
own data via builders/factories/fixtures — the E2E suite assumes a fresh database per run.
