# CLAUDE.md — Operating Rules (Constitution)

This file is always loaded. It contains only the rules that apply to EVERY task.
Detailed guidelines live in `docs/architecture/` and are loaded on demand (see Routing Map).
Inherited architecture decisions live in `docs/DECISIONS-BASELINE.md` (pre-accepted rules).

> Bootstrapping a brand-new project from this foundation? Read `docs/BOOTSTRAP.md` first.

## Non-negotiable invariants

1. **Rule Zero: avoid overengineering.** Architecture must reduce complexity. Patterns,
   layers, abstractions, queues, caches and interfaces exist only when they solve a real
   problem. A simple CRUD stays simple. When in doubt, choose the simplest solution that
   satisfies the spec and tests.
2. **Authority order (conflict resolution):**
   current owner/user request > feature spec > project ADRs > `docs/DECISIONS-BASELINE.md` >
   `docs/architecture/` docs > existing code. Existing code is evidence, not authority. Peer
   preference, fashion and undocumented convention are NOT sources of truth. Never silently
   invent behavior to resolve conflicts — surface the divergence and ask.
3. **Never invent business rules.** If missing information affects behavior, contracts,
   data, security or architecture: ASK the owner before implementing. Record unresolved
   questions under `Open Questions` in the relevant spec. Asking is ALWAYS the default;
   deciding autonomously (recorded via `/dl`) requires the owner's explicit authorization.
4. **Spec-driven development.** Before relevant work: read the applicable spec in
   `docs/specs`; update it if the requirement changed; create one (via `/spec`) if none
   exists. Specs are living artifacts — updated in the same PR as the code they govern.
5. **Tooling is authoritative.** ArchUnit tests, Spring Modulith verify, Spotless, Checkstyle,
   coverage floors, contract snapshots and CI gates encode the architecture rules. Never
   weaken, skip or delete them to make code pass. If a rule seems wrong, propose a change and
   an ADR — do not bypass.
6. **No loose ends.** No TODOs/FIXMEs without an issue/spec/ADR reference, no commented-out
   code, no incomplete implementations, no `@Data`/`@Setter` on JPA entities (Lombok
   `@Getter`/`@RequiredArgsConstructor`/`@Slf4j` are welcome for boilerplate), no `*Impl`
   naming, constructor injection only.
7. **Reference data is registry data, not an enum** (DECISIONS-BASELINE §0019). A new
   business enum is only acceptable when it is a **state machine** (`*Status`/lifecycle),
   **technical** (failure classes, circuit-breaker states) or **fixed by law**. Everything
   else is a registry code (`String` validated by the registry's validator port, seeded by
   migration, branching via `*Codes` constants). When keeping an enum, document the keep
   criterion in the Javadoc.
8. **Quality is non-negotiable: every bug found requires a serious regression test.** Any
   defect — shipped bug, review finding, or anything caught while building/verifying — MUST
   get a regression test that **fails before the fix and passes after**, in **every layer the
   defect can reach** (domain/unit, integration/Testcontainers, API contract, frontend unit,
   E2E). One layer is not enough when the defect spans more; skipping an applicable layer
   requires an explicit stated reason. Details: `docs/architecture/testing.md`.
9. **Git & secret safety** (DECISIONS-BASELINE §0023). Work happens on a `feature/*`/
   `bugfix/*` branch with local commits. **When the slice is complete and green (tested)**,
   the agent **pushes the feature branch and opens a PR targeting `develop`** — the normal
   end of a slice. The agent **NEVER merges to `develop` or `main`** and **never
   force-pushes**; it creates a **tag only on the owner's explicit request**. Merging a PR
   and cutting a release are **human, reviewed** actions (`main` changes only via a release
   PR). Enforced by `.claude/settings.json`. **Never commit a secret, key, certificate or
   `.env`** — gitleaks (CI + pre-commit) blocks them; the only in-repo credentials are
   enumerated dev-only defaults (allowlisted in `.gitleaks.toml`, blocked in prod by a
   fail-fast startup validator).

## Language policy

- **en-US is the default for ALL artifacts**: specs, ADRs, decision log, docs, templates,
  commits, code identifiers. No mixing.
- A document is written in another language **only when the owner names it explicitly**
  (current exception: `docs/GUIA-TIME-CLAUDE.md`, pt-BR).
- **Chat with the owner is in pt-BR** (questions, reports, findings, verdicts).
- End-user-facing language(s) (UI i18n, user manual) are a per-product decision made at
  bootstrap (see `docs/BOOTSTRAP.md`); default en-US. If the product is multilingual, all
  language faces of a document move **in the same slice** — none may lag.

## Definition of Done (every meaningful change)

- Code matches the spec; spec updated if the requirement changed.
- Tests created/updated. Bug fix ⇒ regression test (fails before, passes after); if
  impossible, explain why.
- Flyway migration when schema changes. OpenAPI snapshot/docs updated when contracts change.
- i18n messages added for any user-facing text (in the product's locale(s)). Global error
  handling respected.
- ADR created/updated when architecture changes (inherited rules are revised only via a new
  ADR citing the baseline number).
- User manual (`docs/MANUAL.md`) updated with the slice's user-facing capabilities — run
  `/manual` at the end of every such slice.
- Build and tests executed when possible. Never hide failed commands or skipped checks.

## Final response after implementation

Report: files changed, behavior implemented, specs/ADRs updated, tests, migrations,
contract impacts, commands executed, verification result, risks and pending items.

## Communication during autonomous execution (owner rule)

Whenever running in autonomous/auto-accept mode:

- **Checklist always visible and current** (TodoWrite): one `in_progress` item at a time;
  mark `completed` immediately; the list reflects the real plan.
- **Announce BEFORE each work block** what you are about to do (1-3 lines), and **report
  AFTER** what was actually done and the verification result.
- **Findings, deviations and failures are reported immediately**, never only at the end:
  a bug found, a red test, an autonomous decision taken (with its DL), a scope change.
- **All owner-facing communication in pt-BR** (code, identifiers and commits follow the
  project conventions — en-US).

## Routing Map — read BEFORE touching the area

| If the task involves... | Read first |
|---|---|
| Any non-trivial design decision | `docs/architecture/core-principles.md` |
| Requirement out of scope/undecided; stubbing a future seam | `docs/architecture/simulation-and-mocking.md` |
| Specs, ADRs, plans, large tasks | `docs/architecture/workflow.md` |
| Inherited architecture rules (the pre-accepted baseline) | `docs/DECISIONS-BASELINE.md` |
| Backend code, services, entities, DTOs, errors, dates, naming | `docs/architecture/backend.md` |
| Module boundaries, cross-module calls, REST/JSON contracts, OpenAPI | `docs/architecture/modules-and-apis.md` |
| Events, queues, jobs, schedulers, idempotency, external APIs, files, notifications | `docs/architecture/messaging-and-integrations.md` |
| Database, migrations, transactions, locking, caching, search | `docs/architecture/persistence.md` |
| Security, authorization, user context, privacy, multi-tenancy | `docs/architecture/security.md` |
| Logs, metrics, tracing, performance | `docs/architecture/observability.md` |
| Angular code, components, forms, state, UI | `docs/architecture/frontend-angular.md` |
| Writing or changing tests | `docs/architecture/testing.md` |
| Build, dependencies, Git, CI/CD, Docker, deploy, feature flags | `docs/architecture/delivery.md` |
| Project rituals (spec/ADR/DL scaffolds, slice open/close, release, manual, dev env, CI triage) | the skills in `.claude/skills/` — `/spec` `/adr` `/dl` `/slice` `/dod` `/release` `/manual` `/dev-env` `/ci-triage` `/new-project` |
| Delegating work to the agent team (architect, devs ×N, QA) | the agents in `.claude/agents/` — the architect is the owner's single interlocutor; flow documented in `architect.md` |
| Bootstrapping this foundation into a brand-new project | `docs/BOOTSTRAP.md` |

## Project commands

Use official project commands; inspect `README.md`, `pom.xml`, `package.json` before
inventing any. No system Maven — always use the wrapper from `backend/`:

```bash
cd backend && ./mvnw verify           # backend build + tests (ArchUnit; needs Docker up)
cd backend && ./mvnw spotless:apply   # format
cd frontend && npm run lint && npm test && npm run build   # frontend
```

Destructive and remote operations are governed by `.claude/settings.json` (invariant 9):
pushing a **feature branch** and `gh pr create` (PR → `develop`) are **allowed** as the normal
end of a slice; `git tag` **asks**; `git merge`, `gh pr merge`, `gh release create` and
force-push are **denied**. Do not work around a denied command; explain and ask the owner.

## Command — User manual [`/manual`]

The user manual is a **living artifact**: every slice with user-visible changes is only
"done" when `docs/MANUAL.md` (in the product's locale(s)) reflects the change, in the same
slice. Execution details (structure, label checks against real i18n, screenshot regeneration,
parity verification) live in the skill: run **`/manual`** at the end of every such slice.
