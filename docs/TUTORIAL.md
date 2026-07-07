# TUTORIAL - the Lean SDD slice loop

How every slice of work is built in this project. The skills `/slice` and `/dod` operate
this loop; this document is the canonical reference they read.

## 1. Authority model

The **owner** decides product: approves specs and plans, answers Open Questions, merges PRs
and requests releases. **Claude Code** executes: proposes, implements, tests, documents and
asks whenever information is missing.

Authority order on any conflict:

```text
owner request > feature spec > project ADRs > docs/DECISIONS-BASELINE.md
> docs/architecture/ > existing code
```

Existing code is evidence, not authority.

## 2. Operating model

The main Claude conversation is the default executor. Agents are not used as extra
developers:

- `architect`: helps create specs, ADRs and slice plans.
- `reviewer`: reviews a diff or PR with fresh eyes.
- `qa`: validates risky slices against the spec.

Normal work happens on the current `feature/*` branch. Worktrees are exceptions for risky
spikes, isolated QA or explicit parallel experiments.

## 3. The 7-step loop

```text
0 QUESTIONS -> 1 PLAN -> 2 TEST ANCHOR -> 3 IMPLEMENT -> 4 GATES
-> 5 REVIEW/QA IF RISK -> 6 DoD + PR
```

**0 - QUESTIONS.** Read the spec in full. Any Open Question affecting this slice's behavior
is resolved with the owner before code, or decided under explicit authorized autonomy and
recorded via `/dl`.

**1 - PLAN.** Keep it short and useful: goal, scope, acceptance criteria, implementation
order, test anchor, validation commands, docs to update and risks. Ask for approval when
product behavior, scope or risk changes.

**2 - TEST ANCHOR.** Establish the first credible proof for the slice: a failing regression,
unit/integration/API/frontend/E2E test, API call, command, screenshot or manual check. It
does not need to be religious RED-first, but the slice needs evidence before confidence.

**3 - IMPLEMENT.** Build the smallest vertical slice that satisfies the spec. Follow existing
patterns, update living docs when requirements change, and ask when behavior is ambiguous.

**4 - GATES.** Run focused tests first, then the relevant stack gates. Backend:
`cd backend && ./mvnw verify`. Frontend: `cd frontend && npm run lint && npm test && npm run build`.
E2E is required when a user journey changes. Red gates are fixed in code or architecture,
never by weakening the gate.

**5 - REVIEW/QA IF RISK.** Use `reviewer` for a fresh technical read when the diff is large,
risky or self-directed. Use `qa` for money, LGPD, authorization, audit/retention, clinical
documents, jobs, concurrency, external integrations or broad journeys.

**6 - DoD + PR.** Run `/dod`: verify AC evidence, walk the Definition of Done, update
spec/ADR/manual/status when applicable, push the branch and open a PR to `develop`. The owner
merges.

## 4. Regression micro-loop

Any defect found - shipped, review finding, QA finding or caught while building - gets:

1. a reproducer/test that fails before the fix;
2. the fix;
3. the same test green;
4. coverage at every reachable layer, or an explicit reason why a layer is not applicable.

A fix without its regression evidence does not close.

## 5. One-page summary

Spec first. Questions before code. Small vertical slice. Test anchor. Implement. Gates.
Reviewer/QA only when risk justifies. Living docs updated. PR to `develop`. Owner merges.
