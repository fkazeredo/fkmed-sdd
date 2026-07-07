---
description: >
  Closes a Lean SDD slice: verifies acceptance criteria and test-anchor evidence, runs or
  cites relevant gates, walks the Definition of Done, updates living docs/status, optionally
  invokes Reviewer/QA by risk, then pushes the feature branch and opens a PR to develop.
  Keywords: DoD, definition of done, fechar fatia, gates, open PR.
argument-hint: "[slice-name]"
---

# /dod - close a Lean SDD slice

All communication is in **pt-BR**. Announce slow commands before running them. Never hide a
failed command or skipped check.

## 1. Acceptance and evidence

Recover the slice plan from the conversation or local plan file. For each acceptance
criterion, state:

| AC | Evidence | Why it passes |
|---|---|---|

The evidence can be an automated test, command output, API call, E2E result, screenshot or
manual check. An AC without evidence does not close.

Confirm the **test anchor** named at `/slice` was created/run or explain why a stronger
equivalent replaced it.

## 2. Gates

Run or cite green evidence from the same commit:

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
```

- Start with focused tests, then stack gates.
- E2E is required when a user journey changes:

  ```bash
  cd frontend && npm run e2e:up && npm run e2e && npm run e2e:down
  ```

- PIT/mutation is reserved for money or critical domain logic when useful.
- A red gate means fix the code or architecture; never weaken the gate.
- Reuse green evidence from the same commit instead of rerunning identical expensive gates.

## 3. Definition of Done

Walk `CLAUDE.md` Definition of Done:

- Spec updated if requirement changed.
- Tests created/updated; bug fix has regression coverage.
- Migration added for schema changes; applied migrations not edited.
- OpenAPI snapshot/docs updated when contracts changed.
- i18n updated for user-facing text and error codes.
- ADR created/updated when architecture changed.
- `docs/MANUAL.md` updated when user-visible behavior changed.
- `docs/ROADMAP-STATUS.md` updated for a meaningful closed slice.
- Commands and skipped checks reported honestly.

## 4. Reviewer / QA decision

Before PR, decide explicitly:

- `reviewer` needed? Use for large/risky/self-directed diffs or shared architecture.
- `qa` needed? Use for money, LGPD, authorization, audit/retention, clinical documents,
  jobs, concurrency, integrations or broad user journeys.

If skipped, say why. If run, summarize findings and fixes.

## 5. Optional retrospective report

Do **not** create a versioned conclusion report by default.

Create `docs/reports/final/YYYY-MM-DD-<slice-slug>-final.md` only when the owner asks for a
retrospective or the slice was complex enough that preserving the workflow evidence is
useful. Historical reports remain evidence and should not be rewritten.

## 6. Git closing

- Conventional commits, one purpose per commit.
- `git push -u origin feature/<slice>`.
- `gh pr create --base develop`.
- Never merge the PR, tag or force-push unless the owner explicitly asks for an allowed
  operation.
- PR checks red afterwards? Use `/ci-triage` before changing code.

## 7. Final chat report

Report in pt-BR:

- files changed;
- behavior implemented;
- specs/ADRs/manual/status updated;
- tests and gates run;
- migrations/contracts/i18n impact;
- reviewer/QA outcome if used;
- risks and pending items.
