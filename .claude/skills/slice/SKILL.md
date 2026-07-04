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
5. **Plan** in the `docs/architecture/workflow.md` §Large tasks format: goal, specs, affected
   modules, backend/frontend files, migrations, tests, docs, risks, implementation order,
   validation commands, open questions. Use **plan mode** to present it and get the owner's
   approval.
6. **TodoWrite checklist** mirroring the loop from `docs/TUTORIAL.md` §3:
   `0 PERGUNTAS → 1 PLAN → 2 RED → 3 SKELETON → 4 GREEN → 5 REFACTOR → 6 GATES + DoD`.
7. **Method reminders** (non-negotiable):
   - RED test (acceptance/integration derived from the spec's examples) BEFORE implementation.
   - Skeleton only to compile; minimal green; refactor under green tests.
   - Gates are never weakened to make code pass (invariant 5).
8. **End of the slice** = `/dod` (gates + Definition of Done + PR).
