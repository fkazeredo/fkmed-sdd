# CLAUDE.md - Operating Rules (Constitution)

This file is always loaded. It contains the rules that apply to every task.
Detailed guidelines live in `docs/architecture/` and are loaded on demand through the
Routing Map below. Inherited architecture decisions live in `docs/DECISIONS-BASELINE.md`.

> Bootstrapping a brand-new project from this foundation? Read `docs/BOOTSTRAP.md` first.

## Lean SDD operating mode

1. **Single executor first.** The main Claude conversation is the default executor for a
   slice: understand the spec, plan briefly, implement, test, update docs and close. Do not
   spawn a developer subagent for normal work.
2. **Agents are quality instruments, not extra developers.**
   - `architect`: spec design, ADRs, domain/architecture reasoning and slice decomposition.
   - `reviewer`: fresh technical review of a diff or PR.
   - `qa`: risk-based validation for sensitive or broad slices.
3. **Worktrees are exceptions.** Use a separate worktree only for a risky spike, long
   investigation, isolated QA run, or truly independent parallel experiment approved by the
   owner. The normal path is the current feature branch.
4. **Small vertical slices win.** A slice should produce one observable result and one PR.
   A phase is planning context, not the default execution unit.
5. **Evidence before confidence.** Every meaningful slice needs a test anchor before or
   early in the work: automated test when practical, reproducible command, API call,
   screenshot/manual check, or focused regression reproducer.

## Codex interoperability

This repository also supports Codex. `AGENTS.md` is the Codex entry point and `.agents/skills/`
contains repo-local Codex skills. Keep `.claude/` and `.agents/` as tool-specific wrappers
around the same shared truth: this file, specs, ADRs and architecture docs.

## Non-negotiable invariants

1. **Rule Zero: avoid overengineering.** Architecture must reduce complexity. Patterns,
   layers, abstractions, queues, caches and interfaces exist only when they solve a real
   problem. A simple CRUD stays simple.
2. **Authority order:** current owner/user request > feature spec > project ADRs >
   `docs/DECISIONS-BASELINE.md` > `docs/architecture/` docs > existing code. Existing code
   is evidence, not authority. Never silently invent behavior to resolve conflicts; surface
   the divergence and ask.
3. **Never invent business rules.** If missing information affects behavior, contracts,
   data, security or architecture: ask the owner before implementing. Record unresolved
   questions under `Open Questions` in the relevant spec. Deciding autonomously requires the
   owner's explicit authorization and a decision-log entry.
4. **Spec-driven development.** Before relevant work: read the applicable spec in
   `docs/specs`; update it if the requirement changed; create one with the architect or
   `/spec` if none exists. Specs are living artifacts updated in the same PR as the code
   they govern.
5. **Tooling is authoritative.** ArchUnit tests, Spring Modulith verify, Spotless,
   Checkstyle, coverage floors, contract snapshots and CI gates encode the architecture
   rules. Never weaken, skip or delete them to make code pass. If a rule seems wrong,
   propose a change and an ADR.
6. **No loose ends.** No TODOs/FIXMEs without an issue/spec/ADR reference, no
   commented-out code, no incomplete implementations, no `@Data`/`@Setter` on JPA entities
   (Lombok `@Getter`/`@RequiredArgsConstructor`/`@Slf4j` are welcome for boilerplate), no
   `*Impl` naming, constructor injection only.
7. **Reference data is registry data, not an enum** (DECISIONS-BASELINE §0019). A new
   business enum is only acceptable when it is a state machine (`*Status`/lifecycle),
   technical, or fixed by law. Everything else is a registry code (`String` validated by the
   registry validator port, seeded by migration, branching via `*Codes` constants). When
   keeping an enum, document the keep criterion in Javadoc.
8. **Every bug found requires a serious regression test.** Any defect - shipped bug,
   review finding, or defect caught while building/verifying - must get a regression test
   that fails before the fix and passes after, at every reachable layer. Skipping an
   applicable layer requires an explicit stated reason. Details:
   `docs/architecture/testing.md`.
9. **Git & secret safety** (DECISIONS-BASELINE §0023). Work happens on a `feature/*` or
   `bugfix/*` branch with local commits. When the slice is complete and green, push the
   branch and open a PR targeting `develop`. The agent never merges to `develop` or `main`,
   never force-pushes, and creates tags only on the owner's explicit request. Never commit a
   secret, key, certificate or `.env`.

## Language policy

- **en-US is the default for artifacts**: specs, ADRs, decision log, docs, templates,
  commits and code identifiers.
- A document is written in another language only when the owner names it explicitly
  (current exception: `docs/GUIA-TIME-CLAUDE.md`, pt-BR).
- **Chat with the owner is in pt-BR**.
- End-user-facing language(s) are a product decision made at bootstrap. If the product is
  multilingual, all language faces of a document move in the same slice.

## Definition of Done

- Code matches the spec; spec updated if the requirement changed.
- Tests created/updated. Bug fix implies regression test; if impossible, explain why.
- Flyway migration when schema changes. OpenAPI snapshot/docs updated when contracts change.
- i18n messages added for user-facing text in the product locale(s). Global error handling
  respected.
- ADR created/updated when architecture changes.
- User manual (`docs/MANUAL.md`) updated with user-facing capabilities; run `/manual` when
  applicable.
- `docs/ROADMAP-STATUS.md` receives one concise line for a closed meaningful slice.
- Build and tests executed when possible. Never hide failed commands or skipped checks.

## Final response after implementation

Report in pt-BR: files changed, behavior implemented, specs/ADRs updated, tests, migrations,
contract impacts, commands executed, verification result, risks and pending items.

Do not create a versioned conclusion report by default. Use `docs/reports/final/` only when
the owner asks for a retrospective or the slice was complex enough that preserving the
workflow evidence is useful.

## Communication during execution

- Keep a visible, current checklist for non-trivial work.
- Announce before meaningful work blocks and report after verification.
- Report findings, deviations and failures immediately, not only at the end.
- Owner-facing communication is pt-BR; code, identifiers and commits follow project
  conventions.

## Routing Map - read before touching the area

| If the task involves... | Read first |
|---|---|
| Any non-trivial design decision | `docs/architecture/core-principles.md` |
| Requirement out of scope/undecided; stubbing a future seam | `docs/architecture/simulation-and-mocking.md` |
| Specs, ADRs, slice planning, workflow | `docs/architecture/workflow.md` |
| Inherited architecture rules | `docs/DECISIONS-BASELINE.md` |
| Backend code, services, entities, DTOs, errors, dates, naming | `docs/architecture/backend.md` |
| Module boundaries, cross-module calls, REST/JSON contracts, OpenAPI | `docs/architecture/modules-and-apis.md` |
| Events, queues, jobs, schedulers, idempotency, external APIs, files, notifications | `docs/architecture/messaging-and-integrations.md` |
| Database, migrations, transactions, locking, caching, search | `docs/architecture/persistence.md` |
| Security, authorization, user context, privacy, multi-tenancy | `docs/architecture/security.md` |
| Logs, metrics, tracing, performance | `docs/architecture/observability.md` |
| Angular code, components, forms, state, UI | `docs/architecture/frontend-angular.md` |
| Writing or changing tests | `docs/architecture/testing.md` |
| Build, dependencies, Git, CI/CD, Docker, deploy, feature flags | `docs/architecture/delivery.md` |
| Project rituals | `.claude/skills/` - especially `/spec`, `/slice`, `/dod`, `/manual`, `/ci-triage` |
| Spec/ADR/design help | `.claude/agents/architect.md` |
| Fresh review of a diff or PR | `.claude/agents/reviewer.md` |
| Risk-based validation | `.claude/agents/qa.md` |
| Bootstrapping this foundation into a brand-new project | `docs/BOOTSTRAP.md` |

## Project commands

Use official project commands; inspect `README.md`, `pom.xml`, `package.json` before
inventing any. No system Maven - always use the wrapper from `backend/`:

```bash
cd backend && ./mvnw verify
cd backend && ./mvnw spotless:apply
cd frontend && npm run lint && npm test && npm run build
```

Destructive and remote operations are governed by `.claude/settings.json`: pushing a feature
branch and opening a PR to `develop` are allowed; merging PRs, merging into protected
branches, releases, tags without explicit request and force-pushes are not agent actions.

## Command - User manual [`/manual`]

The user manual is a living artifact: every slice with user-visible changes is only done
when `docs/MANUAL.md` reflects the change in the same slice. Execution details live in the
skill: run `/manual` at the end of such slices.
