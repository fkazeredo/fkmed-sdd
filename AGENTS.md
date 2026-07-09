# AGENTS.md - Codex instructions for FKMed

This repository is shared between Claude Code and Codex.

## Source of truth

- Treat `CLAUDE.md` as the project constitution for FKMed work.
- Use `docs/specs/`, `docs/adr/`, `docs/decision-log/`, `docs/architecture/`,
  `docs/ROADMAP.md` and `docs/ROADMAP-STATUS.md` as living project authorities.
- If project files conflict, follow the authority order in `CLAUDE.md`.
- If `CLAUDE.md` conflicts with Codex system/developer/user instructions, the Codex
  instructions win; explain the conflict instead of silently ignoring the project rule.

## How Codex should work here

- Use FKMed Lean SDD for non-trivial work:
  `QUESTIONS -> PLAN -> TEST ANCHOR -> IMPLEMENT -> GATES -> REVIEW/QA IF RISK -> DoD + PR`.
- Before implementation, read `CLAUDE.md`, `docs/architecture/workflow.md`, the relevant
  spec/plan and the relevant architecture docs from the Routing Map.
- The main Codex conversation is the default executor. Do not create developer subagents or
  worktrees unless the owner explicitly asks or the risk clearly justifies it.
- Use repo skill `$fkmed-lean-sdd` when implementing a slice, converting a `.md` plan into
  code, closing a slice, or aligning a task with FKMed Lean SDD.
- Authorization to implement or finish a slice includes conventional commit(s), pushing its
  feature branch and opening the PR to `develop` after green gates. Do not ask again unless the
  owner explicitly says `local-only` or `no PR`; `draft` means open a Draft PR.
- Chat with the owner in pt-BR. Keep code identifiers, commits, specs and ADRs in en-US
  unless a project doc explicitly says otherwise.

## Claude Code interoperability

- Claude Code uses `CLAUDE.md` and `.claude/`.
- Codex uses this `AGENTS.md` and repo skills in `.agents/skills/`.
- Do not delete `.claude/` files just because Codex has an equivalent surface.
- The shared truth is the product/architecture documentation, not the tool-specific wrapper.

## Safety

- Do not touch unrelated untracked files or user work.
- Never merge to protected branches, tag, force-push or weaken gates. A normal feature-branch push
  and PR creation are required workflow closure, not protected-branch merge authorization.
- If behavior, contract, data, security or architecture is ambiguous, ask the owner before
  coding.
