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

## Team orchestration & agent discipline

When the architect delegates to sub-agents (devs, QA), the orchestration rules that keep the
process from bottlenecking live in `.claude/agents/architect.md` (owner-facing summary in
`docs/GUIA-TIME-CLAUDE.md`). In short:

- **Parallel by default, contract-first** — two axes, treated differently. (a) **Backend ×
  frontend:** for any end-to-end slice, running `dev-backend` and `dev-frontend` in parallel
  is simply the default; the architect **freezes the API contract in the plan** (endpoints,
  DTO shapes, error codes, events, state/session behavior) and **partitions the work into
  disjoint files/modules** — that contract + those boundaries are what keep the two sides from
  colliding — then integrates both sub-branches into the slice branch (`git merge --no-ff`;
  targeted check per merge, the full battery once after the last integration). (b) **N
  instances of one specialty:** spawn another `dev-backend`
  (or `dev-frontend`) only when there is a genuinely disjoint scope that earns its keep —
  judgment by real demand, never idle instances (Rule Zero). A genuinely small slice is done
  inline; sequential cross-stack is the deliberate exception (emergent contract, or a trivial
  side). A backend deviation from the frozen contract is an impediment back to the architect,
  never a silent drift.
- **Execution modes & proportional gates** (canonical text in `.claude/agents/architect.md`
  §Execution modes) — **Slice Mode is the default**; a **whole phase** runs when the owner
  explicitly asks for one (accepted without pushback, organized internally in waves). Parallel
  work requires the plan to fix the frozen contract, owned/forbidden paths, the
  **single-writer surfaces** (OpenAPI snapshot, migration numbering, shell/routes, global
  i18n, `ModularityTest`, shared error mapping, workflows) and the merge order. Gates are
  **proportional**: devs run targeted tests during the loop and their stack's full gate once
  at handback; the architect runs the full battery once after the final integration; QA runs
  once on the integrated branch; `/dod` reuses green evidence from the same commit instead of
  re-running it. The E2E suite is **green locally before any push/PR** — CI is never its
  first run (owner order, the Phase-4 lesson). Effort defaults to `high` everywhere;
  `opus`/`xhigh` are escalations for genuinely hard/critical work, never a session default.
- **Worktree orchestration** — each agent works in **its own worktree**, never the main repo
  or another's. The **architect owns the lifecycle**: free the target branch and keep the
  main worktree on `develop` before spawning (a branch held elsewhere makes the agent's
  checkout fail); **physically remove finished worktrees** after (not just `git worktree
  prune` — on Windows `rm -rf .claude/worktrees/<id>` in Git Bash when path length blocks
  `git worktree remove`), delete merged/scratch branches, and leave no file junk on the
  owner's machine; rescue any misplaced work via `git stash -u` without losing it.
- **Visibility** — a milestone-ping cadence (RED committed → gates green → completion) for
  devs, QA and flow work; the architect surfaces observable state, never invented progress;
  periodic stall checks.
- **Impediments escalate, never route around** — any blocker not the agent's to fix (failed
  checkout, unavailable tool/service, ambiguous/conflicting spec, a gate that looks wrong,
  scope bigger than the order) is **reported to the architect and waits**; agents never fake
  a result, weaken a gate, invent a rule, or work outside their scope/worktree to force a
  pass. A reported impediment comes to the architect to resolve (escalation ladder rung 0).
- **Out-of-scope findings** QA raises don't fail the slice but are **not dropped** — they come
  to the architect to analyze and disposition (spec item, future slice, replan, or noted).

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
