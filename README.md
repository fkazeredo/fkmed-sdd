# Boilerplate — Spec-Driven Development foundation

A portable, **domain-free** foundation distilled from a real project built end-to-end with
Claude Code under Spec-Driven Development: its constitution, architecture rules, battle-tested
decisions, workflow, quality gates and agent team — **without any of its business content**.
Use it to start a new product with the same architecture, code design and quality bar.

## What's inside

| File / folder | What it is |
|---|---|
| `CLAUDE.md` | The constitution — always-loaded operating rules (~150 lines; the only file that costs context on every request) |
| `.claude/skills/` | 10 ritual commands: `/spec` `/adr` `/dl` `/slice` `/dod` `/release` `/manual` `/dev-env` `/ci-triage` `/new-project` |
| `.claude/agents/` | The agent team: `architect` (the owner's single interlocutor) + `dev-backend`/`dev-frontend`/`dev-fullstack` + `qa` |
| `.claude/settings.json` | Git guardrail: agents push feature branches and open PRs; never merge/tag/force-push |
| `docs/BOOTSTRAP.md` | The build recipe: empty repo → green walking skeleton (stack, gates, CI, sequence) |
| `docs/DECISIONS-BASELINE.md` | 21 inherited architecture decisions as pre-accepted rules (with provenance) |
| `docs/architecture/` | The 13 detailed rule documents (backend, frontend, testing, security, persistence…) — loaded on demand via the Routing Map |
| `docs/TUTORIAL.md` | The 7-step slice loop (RED test first → gates + Definition of Done) |
| `docs/RUN-PHASE.md` | The decision-log format + authorized-autonomy rules |
| `docs/specs/0000-specs-template.md` | Spec template, optimized for LLM execution (testable BRs, Given/When/Then acceptance criteria, Open Questions) |
| `docs/adr/0000-adr-template.md` | ADR template (with revision triggers) |
| `docs/GUIA-TIME-CLAUDE.md` | Didactic guide to operating the agent team (pt-BR, by owner request) |

## How to use (3 steps)

1. **Copy the CONTENTS of this folder to the root of your new repository** (including the
   hidden `.claude/`).
2. Open Claude Code in the new repo: `claude` (or `claude --agent architect` to start with
   the coordinator persona).
3. Say: **"Read docs/BOOTSTRAP.md and let's start."** The architect will collect your product
   decisions (domain, package, languages), write SPEC-0001 with you and build the walking
   skeleton — gates and CI included.

From then on, every feature follows the loop: spec (with you) → plan (you approve) → build
(agent team) → QA → review → PR (**you merge**). The didactic guide is
`docs/GUIA-TIME-CLAUDE.md`.

## Design notes

- **Token-conscious:** only `CLAUDE.md` is always loaded. Everything else (architecture docs,
  baseline, skills' bodies, agents' bodies) loads on demand.
- **Language policy:** en-US for all artifacts by default; pt-BR only where the owner named
  it (the guide). Chat with the owner is pt-BR.
- **No product code inside:** the application is born via walking skeleton (BOOTSTRAP §5),
  not copied — so nothing from the parent project's business leaks into yours.
- **Inherited decisions are revisable:** write a new ADR citing the baseline number
  (see `docs/DECISIONS-BASELINE.md`, header).
