---
description: >
  Opens a Lean SDD slice: validates the spec, resolves Open Questions, creates/confirms the
  feature branch from develop, builds a short plan, chooses a test anchor and identifies
  whether Architect/Reviewer/QA are needed. Use when starting any meaningful feature or fix
  that has a spec.
  Keywords: fatia, slice, começar feature, start implementation, new task.
argument-hint: <SPEC-NNNN> [short-slice-name]
---

# /slice - open a Lean SDD slice

All communication with the owner is in **pt-BR**. Keep the owner oriented, but keep the
process light.

## Steps

1. **Read the target spec in full** (`docs/specs/NNNN-*.md`, from argument `$0`). If no spec
   exists for the topic, offer to create one with the `architect` or `/spec`, then stop.
2. **Open Questions gate:** if an Open Question affects this slice's behavior, contract,
   data, security or architecture:
   - default: stop and ask the owner;
   - authorized autonomy only: decide, record via `/dl`, then code.
3. **Read the relevant rules** from `CLAUDE.md`'s Routing Map. List only the docs that
   actually mattered.
4. **Git:** create or confirm `feature/<slice-slug>` from `develop`.
   - Normal work stays in the current feature branch.
   - Do not create worktrees unless the owner explicitly asks for an isolated experiment,
     long spike, isolated QA pass or true parallel work.
5. **Short plan** in the conversation:

   ```text
   Goal
   Spec(s)
   Scope / out of scope
   Acceptance criteria
   Implementation order
   Test anchor
   Validation commands
   Docs to update
   Risks / open questions
   ```

6. **Test anchor:** name the first evidence that will prove the slice. It can be a failing
   regression test, unit/integration/API/frontend/E2E test, API call, command, screenshot or
   manual check. The anchor must be credible for the risk.
7. **Agent decision:**
   - `architect` before implementation only for spec/ADR/design uncertainty.
   - `reviewer` after implementation for large, risky or self-directed diffs.
   - `qa` only for money, LGPD, authorization, audit/retention, clinical documents, jobs,
     concurrency, external integrations or broad user journeys.
8. **Checklist** for the slice:

   ```text
   0 QUESTIONS
   1 PLAN
   2 TEST ANCHOR
   3 IMPLEMENT
   4 GATES
   5 REVIEW/QA IF RISK
   6 DoD + PR
   ```

9. **End of the slice** = `/dod`.

## Reminders

- The main Claude conversation is the executor by default.
- Never invent business rules.
- Every bug fixed gets a regression test.
- Gates are never weakened to make code pass.
- Persisting a plan file under `docs/reports/plans/` is optional and local-only.
