# Workflow: Specs, ADRs, Plans and Documentation

> Read when: writing/updating specs or ADRs, planning a large task, creating a new project,
> or updating README/runbooks.

## Specs (`docs/specs`)

Specs are mandatory for: new features, business rules, workflows, state transitions,
integrations, messaging, domain events, real-time behavior, AI/model behavior, persistence
changes, API contracts, security-sensitive flows, important frontend behavior, background
jobs, caching, error handling strategy and architectural refactors.

A good spec **MUST** include: goal, scope, business context, business rules, input/output
examples, API contracts and events when applicable, persistence changes, validation rules,
error behavior, observability requirements, required tests, acceptance criteria and open
questions. Use `docs/specs/0000-specs-template.md` (or the `/spec` command).

Specs **MUST** use direct, testable language:

```txt
Good: The system MUST reject cancellation when the order status is INVOICED.
      The API MUST return 409 with error code order.cannot-be-cancelled.
Bad:  Handle cancellation properly. Improve validation.
```

## ADRs (`docs/adr`)

Create an ADR when a decision: affects architecture; introduces/removes a major dependency;
changes module boundaries; defines persistence/messaging/integration strategy; affects
deployment or scalability; introduces caching or eventual consistency; changes security
approach; creates meaningful trade-offs; or would be expensive to reverse.
Use `docs/adr/0000-adr-template.md` (or the `/adr` command).

## Decision log (`docs/decision-log`) and changelogs

Decisions taken **autonomously** (gaps and Open Questions resolved without the owner in the
room) are recorded as append-only `DL-NNNN-title.md` files with confidence and
reversibility ratings, indexed in `INDEX.md` — see `docs/RUN-PHASE.md` for the exact format.
Release notes live as a consolidated changelog (`docs/release-notes/CHANGELOG.md`, one face
per product locale if multilingual); per-slice test results are recorded in the execution
log (`docs/ROADMAP-STATUS.md`).

## Large tasks

Use Claude Code plan mode. The plan **MUST** include: goal, relevant specs, affected modules,
backend/frontend files, migrations, tests, documentation, architectural risks, implementation
order, validation commands, open questions and **acceptance criteria** — numbered (AC-1…),
testable, mapped to the spec's BRs/examples, each with its verification method. At the end of
the slice (`/dod`) every AC is re-verified with evidence and a detailed why.

## Slice reports (`docs/reports`)

Two per-slice reports, written by the architect (naming and gitignore rule in
`docs/reports/README.md`):

- **Plan report** (`docs/reports/plans/`, NOT versioned): the approved plan with its
  acceptance criteria — written at `/slice`.
- **Conclusion report** (`docs/reports/final/`, versioned, committed in the slice PR): the
  AC table with evidence and whys + a workflow retrospective (handoff timeline, reworks,
  bottlenecks, lessons learned) — written at `/dod`, in pt-BR.

## Repository context awareness

Before a relevant change, inspect: project structure, `CLAUDE.md`, `docs/architecture/`, relevant
specs and ADRs, existing modules, package conventions, tests, API patterns, error handling,
i18n strategy, migrations, frontend patterns, scripts. **MUST NOT** introduce a parallel
architecture when an equivalent mechanism exists — search for existing equivalents
(`ApiErrorResponse`, `UserContextProvider`, `FileStorage`, ...) before creating new ones.

## README and runbooks

README is an operational entry point: purpose, stack, structure, setup, env vars, how to run
backend/frontend/tests/migrations, common commands, links to `CLAUDE.md`/`docs/architecture/`/
specs/ADRs, troubleshooting. Not an encyclopedia — link out.

Runbooks **SHOULD** exist when operational risk is relevant: deployment, rollback, migration
failure, integration outage, queue backlog, DLQ reprocessing, job failure, cache
inconsistency, WebSocket degradation, AI provider unavailable, high error rate/latency,
database unavailable.

## New project creation

Start from an initial product/spec context and generate a minimal functional structure that
runs, tests and follows the architecture. **MUST NOT** create a huge empty architecture with
unused modules, fake bounded contexts or placeholder classes.

Sequence: read/create initial spec → identify initial domains → generate minimal runnable
backend/frontend → documentation entry points → local dev setup (`docker-compose`,
`.env.example`) → basic tests → initial CI when appropriate. The first feature **MUST** be
guided by a spec.

Slices land via a **branch + Pull Request** to `develop`; `main`/`develop` are protected and change
only via reviewed PR — agents push the feature branch and open the PR but **never merge to protected
branches** (see `CONTRIBUTING.md`).
