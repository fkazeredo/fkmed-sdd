---
description: >
  Opens a new slice through the TUTORIAL's 7-step loop: validates the spec and its Open
  Questions, creates the feature branch from develop, builds the slice plan and the loop
  checklist (RED test first). Use when starting any slice/feature/fix that has a spec.
  Keywords: fatia, slice, começar feature, start implementation, new task.
argument-hint: <SPEC-NNNN> [short-slice-name]
---

# /slice — open a slice

All communication with the owner is in **pt-BR**: announce BEFORE each block what you are
going to do, report AFTER what you did (CLAUDE.md §Comunicação).

## Steps

1. **Read the target spec IN FULL** (`docs/specs/NNNN-*.md`, from argument `$0`). If no spec
   exists for the topic, offer to create one via `/spec` and **stop** (invariant 4 — no
   relevant work without a spec).
2. **Open Questions gate (invariant 3):** if the spec has an Open Question affecting this
   slice's behavior:
   - Default: **STOP and ask the owner** (asking is always the default — owner rule).
   - Only if the owner explicitly authorized an autonomous run: decide and record via `/dl`
     BEFORE coding.
3. **Read the area's rules**: consult the `CLAUDE.md` Routing Map and read the
   `docs/architecture/` docs for the areas the slice touches (backend, frontend, persistence,
   testing…). List for the owner which ones you read.
4. **Git**: `git checkout develop && git pull --ff-only`, then
   `git checkout -b feature/<slice-slug>` (history convention: short kebab slug).
   **Worktree orchestration (architect.md):** before you spawn ANY agent, switch the **main
   worktree back to `develop`** and make sure the slice branch (or the dev's sub-branch) is
   **not checked out anywhere** — git allows a branch in only one worktree, so a branch you
   still hold makes the agent's checkout fail and it may fall back to the wrong directory.
   Keep the main worktree off the agents' branches the whole time they run.
5. **Plan** in the `docs/architecture/workflow.md` §Large tasks format: goal, specs, affected
   modules, backend/frontend files, migrations, tests, docs, risks, implementation order,
   validation commands, open questions — and a **Critérios de aceite** section: numbered
   (AC-1, AC-2, …), testable, each mapped to the spec's BRs/examples and stating its
   verification method (test name, command, API call). These ACs are re-verified one by one
   at `/dod`, with evidence and the detailed why. Use **plan mode** to present it and get
   the owner's approval.

   **Execution mode (architect.md §Execution modes):** Slice Mode is the default. When the
   owner explicitly asks for a **whole phase**, accept it — don't argue for smaller PRs —
   and organize the same plan internally in waves. When work will run in **parallel**, the
   plan additionally fixes: the frozen contract, each dev's owned/forbidden paths, the
   **single-writer surfaces** (OpenAPI snapshot, migrations numbering, shell/routes, global
   i18n, `ModularityTest`, shared error mapping, workflows — only the architect or one
   assigned dev touches them), and the **merge order** of the sub-branches.
6. **Persist the approved plan** to `docs/reports/plans/YYYY-MM-DD-<slice-slug>-plan.md`
   (NOT versioned — the folder is gitignored; see `docs/reports/README.md`). When the
   architect will run devs in parallel, the plan also names the sub-branches
   (`feature/<slice>--<scope>`) each dev will use.
7. **TodoWrite checklist** mirroring the loop from `docs/TUTORIAL.md` §3:
   `0 PERGUNTAS → 1 PLAN → 2 RED → 3 SKELETON → 4 GREEN → 5 REFACTOR → 6 GATES + DoD`.
8. **Method reminders** (non-negotiable):
   - RED test (acceptance/integration derived from the spec's examples) BEFORE implementation.
   - Skeleton only to compile; minimal green; refactor under green tests.
   - Gates are never weakened to make code pass (invariant 5).
9. **End of the slice** = `/dod` (gates + Definition of Done + AC evidence + retrospective + PR).
