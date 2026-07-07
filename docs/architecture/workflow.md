# Workflow: Lean SDD

> Read when: writing/updating specs or ADRs, opening a slice, closing a slice, reviewing a
> PR, or changing project documentation.

## Core model

FKMed uses **Lean SDD**:

```text
spec/design -> small vertical slice -> test anchor -> implementation -> gates -> review/QA if risk -> PR
```

The main Claude conversation is the default executor. Agents are used only when they reduce
cognitive load or increase confidence:

- `architect`: spec design, ADRs, domain/architecture reasoning and slice decomposition.
- `reviewer`: fresh technical review of a diff/PR.
- `qa`: risk-based validation for sensitive or broad slices.

There is no default developer subagent and no default worktree orchestration.

## Specs (`docs/specs`)

Specs are mandatory for: new features, business rules, workflows, state transitions,
integrations, messaging, domain events, real-time behavior, AI/model behavior, persistence
changes, API contracts, security-sensitive flows, important frontend behavior, background
jobs, caching, error handling strategy and architectural refactors.

A good spec must include: goal, scope, business context, business rules, input/output
examples, API contracts and events when applicable, persistence changes, validation rules,
error behavior, observability requirements, required tests, acceptance criteria and open
questions. Use `docs/specs/0000-specs-template.md` or `/spec`.

Specs must use direct, testable language:

```text
Good: The system MUST reject cancellation when the order status is INVOICED.
      The API MUST return 409 with error code order.cannot-be-cancelled.
Bad:  Handle cancellation properly. Improve validation.
```

The `architect` agent is the preferred assistant for creating or improving specs. It may
ask questions, draft the spec and recommend slices, but implementation starts only on the
owner's explicit order.

## ADRs (`docs/adr`)

Create an ADR when a decision affects architecture; introduces/removes a major dependency;
changes module boundaries; defines persistence/messaging/integration strategy; affects
deployment or scalability; introduces caching or eventual consistency; changes security
approach; creates meaningful trade-offs; or would be expensive to reverse.

Use `docs/adr/0000-adr-template.md` or `/adr`.

## Decision log (`docs/decision-log`) and changelogs

Decisions taken autonomously are recorded as append-only `DL-NNNN-title.md` files with
confidence and reversibility ratings, indexed in `INDEX.md`; see `docs/RUN-PHASE.md`.

Release notes live in `docs/release-notes/CHANGELOG.md`. Per-slice execution summaries live
in `docs/ROADMAP-STATUS.md`.

## Slice size

A slice should normally have:

- one user/business outcome;
- one feature branch;
- one PR to `develop`;
- one test anchor;
- one concise status update when closed.

Prefer vertical slices: migration -> domain -> API -> screen, when the work is product
code. Backend-only or frontend-only work is acceptable only when the result is inherently
technical, preparatory, or explicitly requested.

Avoid whole-phase execution as the default. If the owner asks for a whole phase, decompose it
into small internal slices and keep the execution sequential unless true parallel isolation
is worth the coordination cost.

## Opening a slice

Use `/slice` or follow the same structure:

1. Read the spec in full.
2. Resolve blocking Open Questions with the owner, or record an authorized autonomous
   decision via `/dl`.
3. Read the relevant architecture docs from `CLAUDE.md`'s Routing Map.
4. Create or confirm a `feature/<slice-slug>` branch from `develop`.
5. Write a short plan:
   - goal;
   - spec(s);
   - scope/out of scope;
   - acceptance criteria;
   - implementation order;
   - test anchor;
   - validation commands;
   - docs to update;
   - risks/open questions.
6. Get owner approval when the plan changes product behavior, scope or risk.

Persisting a plan file is optional. Prefer the conversation checklist unless the slice is
large enough that a local plan file is useful.

## Test anchor

Before or early in implementation, establish at least one anchor that proves the slice:

- unit/integration/API/frontend/E2E test;
- failing regression reproducer for a bug;
- API call or command with expected output;
- screenshot/manual path for UI-only behavior.

This is not a religious RED-first rule. It is a confidence rule: do not build a meaningful
slice with no way to prove it.

## Implementation

The main Claude executor implements the slice on the current feature branch:

- follow the existing architecture and local patterns;
- prefer backend contract reality over imagined frontend contracts;
- update specs/ADRs/manual/changelog only when the slice changes them;
- keep commits conventional and scoped;
- ask the owner when behavior is ambiguous.

Worktrees are exceptions for risky spikes, long investigations, isolated QA, or explicitly
approved parallel experiments. They are not part of the normal workflow.

## Gates

Run the cheapest commands that provide real confidence, then broaden as risk grows:

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
cd frontend && npm run e2e:up && npm run e2e && npm run e2e:down
```

Rules:

- Start with focused tests when possible; run stack gates before closing.
- E2E is required when a user journey changes.
- PIT/mutation is reserved for money or critical domain logic when useful.
- Red gate means fix the code or architecture, never weaken the gate.
- Reuse green evidence from the same commit instead of rerunning identical expensive gates.

## Reviewer and QA

Use `reviewer` when the diff is large, shared, risky or self-directed enough to benefit from
fresh eyes. Reviewer is read-only by default and should lead with findings.

Use `qa` for money, LGPD, authorization, audit/retention, clinical documents, jobs,
concurrency, external integrations or broad cross-stack user journeys. QA validates against
the spec and reports evidence; it does not fix production code by default.

## Closing a slice

Use `/dod` or follow the same structure:

1. Confirm acceptance criteria and test anchor evidence.
2. Run/cite the relevant gates.
3. Check the Definition of Done in `CLAUDE.md`.
4. Update spec/ADR/manual/changelog/ROADMAP-STATUS when applicable.
5. Run reviewer/QA if the risk triggers them or the owner asks.
6. Push the feature branch and open a PR to `develop`.
7. Do not merge, tag or force-push.

`docs/reports/final/` is optional for complex retrospectives only. It is not a mandatory
per-slice artifact.

## Repository context awareness

Before a relevant change, inspect project structure, `CLAUDE.md`, relevant architecture
docs, specs and ADRs, existing modules, package conventions, tests, API patterns, error
handling, i18n strategy, migrations, frontend patterns and scripts.

Do not introduce a parallel architecture when an equivalent mechanism exists; search for
existing equivalents before creating new ones.

## README and runbooks

README is an operational entry point: purpose, stack, structure, setup, env vars, how to run
backend/frontend/tests/migrations, common commands, links to `CLAUDE.md`, architecture docs,
specs and ADRs, troubleshooting. Not an encyclopedia; link out.

Runbooks should exist when operational risk is relevant: deployment, rollback, migration
failure, integration outage, queue backlog, DLQ reprocessing, job failure, cache
inconsistency, WebSocket degradation, AI provider unavailable, high error rate/latency, or
database unavailable.

## New project creation

Start from an initial product/spec context and generate a minimal functional structure that
runs, tests and follows the architecture. Do not create a huge empty architecture with unused
modules, fake bounded contexts or placeholder classes.

The first feature must be guided by a spec. Slices land through a branch and Pull Request to
`develop`; protected branches change only via reviewed PR.
