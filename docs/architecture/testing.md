# Testing

> Read when: writing or changing tests, deciding test strategy or closing a slice.

## Philosophy

Tests protect behavior, prevent regressions and document expectations. Coverage is a signal, not
the goal. A weak assertion with high coverage is still weak quality.

Every defect found while building, reviewing or verifying needs a regression test at every layer the
defect can realistically reach. Skipping a layer requires a clear reason in the final response.

## Pyramid and Gates

| Level | Tooling | Gate |
|---|---|---|
| Domain/unit | JUnit 5 | part of `./mvnw verify` |
| Integration/API/security/events | Spring test + Testcontainers/Postgres + `TestJwtTokens` | part of `./mvnw verify` |
| Architecture | ArchUnit + Spring Modulith `verify()` | part of `./mvnw verify` |
| Contract | OpenAPI snapshot + module diagram snapshot | part of `./mvnw verify` |
| Coverage | JaCoCo instruction/branch floors | part of `./mvnw verify` |
| Mutation | PIT profile for money/critical domain rules | `./mvnw -Pmutation ...` when useful |
| Frontend unit | Vitest + jsdom + coverage floors | `npm test` |
| Frontend lint/build | angular-eslint + Angular build budgets | `npm run lint`, `npm run build` |
| E2E | Playwright on `compose.e2e.yaml` isolated stack | `npm run e2e` |

Latest local evidence after the post-Phase 6 hardening branch: backend verify 763 tests, frontend
lint/test/build with 524 frontend tests, and `git diff --check`. Latest recorded Phase 6 E2E
evidence remains 33 Playwright journeys; E2E was not rerun for the hardening branch because no user
journey or frontend behavior changed. This branch adds focused backend regressions for upload
transport limits, masked authentication logs and correlation IDs.

## Backend Testing

Prioritize:

- domain invariants and state transitions;
- service orchestration and business exceptions;
- persistence, transactions and locking;
- API contracts, status codes and error codes;
- security-sensitive 401/403/404/409/422 paths;
- event publication/listeners and notification side effects;
- uploads and file-content validation;
- time/date boundaries.

Use Testcontainers when database behavior matters. Use unit tests for pure domain logic. Do not
weaken ArchUnit, Modulith, snapshot or coverage gates to make a change pass.

## Frontend Testing

Protect feature services, guards, validators, submit flows, error handling, permission behavior,
loading/empty/error states and critical user journeys. E2E is for critical flows, not every tiny UI
branch.

When a backend contract changes, update frontend tests and OpenAPI-aware code together.

## Regression Layers

A wrong upload limit can need:

- config test for transport limits;
- API/integration test for domain error path if the request reaches the backend;
- frontend test for client-side blocking if visible.

A wrong authorization rule can need:

- integration/API test for denied access;
- frontend guard/menu test when UI can reach it;
- E2E if it breaks a critical user journey.

A UI-only validation or copy defect may only need a frontend unit/E2E test, as long as the backend
contract was already correct.

## Manual QA

Manual QA complements automation. Use `docs/QA-CADERNO-DE-TESTES.md` for the human test book:
coverage by feature, exploratory charters, security/privacy probes, accessibility checks and
evidence templates.

## Test Environments

Never run E2E against a developer database. Playwright uses `compose.e2e.yaml` with fresh isolated
data. Tests must create or reset their own data and avoid relying on mutable global state.
