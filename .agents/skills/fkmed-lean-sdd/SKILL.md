---
name: fkmed-lean-sdd
description: FKMed Lean SDD workflow for implementing, reviewing, or closing slices from specs, ADRs, docs/plans, or owner prompts. Use when Codex is asked to execute a FKMed slice, convert a Markdown plan into code, follow CLAUDE.md, run /slice or /dod semantics, decide whether Reviewer/QA is needed, or prepare a PR for develop.
---

# FKMed Lean SDD

Use this skill to execute FKMed work with the repository's shared Claude/Codex workflow.
Do not run Claude Code slash commands literally; apply the documented semantics.

## Load context

1. Read `AGENTS.md`.
2. Read `CLAUDE.md`.
3. Read `docs/architecture/workflow.md`.
4. Read the task's primary Markdown input: spec, ADR, plan or roadmap item.
5. Read only the relevant architecture docs from the `CLAUDE.md` Routing Map.

## Execution loop

Follow:

```text
0 QUESTIONS
1 PLAN
2 TEST ANCHOR
3 IMPLEMENT
4 GATES
5 REVIEW/QA IF RISK
6 DoD + PR
```

## Questions

Stop and ask the owner when missing information affects behavior, contracts, data,
security or architecture. Under explicit authorized autonomy, record decisions through the
project decision-log convention before coding.

## Plan

Keep the plan short:

- goal;
- spec(s)/plan(s);
- scope and out of scope;
- acceptance criteria;
- implementation order;
- test anchor;
- validation commands;
- docs to update;
- risks and open questions.

## Test anchor

Establish at least one credible proof before or early in implementation: failing regression,
unit/integration/API/frontend/E2E test, API call, command, screenshot or manual check.

## Implement

Use the main Codex conversation as the executor. Prefer existing code patterns and local
helpers. Do not create worktrees or parallel agents by default.

## Gates

Run focused tests first, then gates proportionally:

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
cd frontend && npm run e2e:up && npm run e2e && npm run e2e:down
cd backend && ./mvnw -Pmutation org.pitest:pitest-maven:mutationCoverage
```

Backend/frontend gates run when those stacks changed. E2E runs when a user journey changed.
PIT/mutation is for money or critical domain rules when useful.

## Reviewer and QA

Use a review stance for broad/risky/self-directed diffs. Use QA thinking for money, LGPD,
authorization, audit/retention, clinical documents, jobs, concurrency, external
integrations or broad user journeys.

## Close

Before final response or PR:

- map acceptance criteria to evidence;
- update spec/ADR/manual/changelog/status when applicable;
- report commands run and skipped checks honestly;
- never merge PRs, tag or force-push unless the owner explicitly asks for an allowed action.
