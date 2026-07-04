# TUTORIAL — the per-slice method (the 7-step loop)

How every slice of work is built in this project. The skills `/slice` and `/dod` operate this
loop; this document is the canonical reference they read.

## 1. Authority model

The **owner** decides product: approves specs and plans, answers Open Questions, merges PRs,
requests releases. **Claude Code** executes: proposes, implements, tests, documents — and
**asks whenever information is missing** (it never invents business rules). Authority order
on any conflict: owner request > feature spec > project ADRs > `docs/DECISIONS-BASELINE.md` >
`docs/architecture/` > existing code.

## 2. One-time setup

The project starts from `docs/BOOTSTRAP.md` (stack, gates, CI, walking skeleton). After that,
all work happens in slices, each on a `feature/*` branch, each ending in a PR to `develop`.

## 3. The 7-step loop (every slice)

```
0 QUESTIONS → 1 PLAN → 2 RED → 3 SKELETON → 4 GREEN → 5 REFACTOR → 6 GATES + DoD
```

**0 — QUESTIONS.** Read the spec in full. Any Open Question affecting this slice's behavior
is resolved WITH the owner before anything else (or, under explicitly authorized autonomy,
decided and recorded via `/dl` — see `docs/RUN-PHASE.md`). The answer is written back into
the spec (`Open Questions` → `Business Rules`).

**1 — PLAN.** Plan mode, in the format of `docs/architecture/workflow.md` §Large tasks: goal,
specs, affected modules, files, migrations, tests, docs, risks, implementation order,
validation commands, open questions. The owner approves before any code.

**2 — RED.** Write the acceptance/integration test derived from the spec's Business Rules and
I/O examples — and watch it FAIL. No implementation before a failing test. This test is the
spec made executable.

**3 — SKELETON.** The minimum types/ports/empty migration for the test to compile. Nothing
more — no speculative structure (Rule Zero).

**4 — GREEN.** The minimum implementation that makes the test pass. Resist gold-plating.

**5 — REFACTOR.** Clean up under green tests: naming, duplication, structure. Behavior does
not change in this step.

**6 — GATES + DoD.** Run `/dod`, which enforces:

- [ ] `cd backend && ./mvnw verify` green (Spotless, Checkstyle, JaCoCo floors, ArchUnit,
      Modulith + diagram snapshot, OpenAPI snapshot, i18n/HTTP-mapping completeness).
- [ ] `cd frontend && npm run lint && npm test && npm run build` green.
- [ ] E2E green when the slice touches a user journey (isolated stack only).
- [ ] Spec updated if the requirement changed during the slice.
- [ ] Bug fixed ⇒ regression test failing-before/passing-after at EVERY reachable layer.
- [ ] Flyway migration for any schema change (never edit an applied one).
- [ ] i18n messages for any user-facing text, in the product's locale(s), with parity.
- [ ] User manual updated (`/manual`) if anything user-visible changed.
- [ ] Version bumped (`/release`) if code changed; docs-only slices never bump.
- [ ] Conventional Commits; push feature branch; PR to `develop`. Never merge/tag.

## 4. The regression micro-loop (bugs)

Any defect found (shipped, review finding, or caught while building): (1) write the test that
REPRODUCES it — red; (2) fix — green; (3) repeat the test at every other layer the defect can
reach (domain/integration/API/frontend/E2E); (4) only then close. A fix without its regression
test does not merge.

## 5. One-page summary

Spec first, questions before code, plan approved by the owner, failing test before
implementation, minimum to green, refactor under green, gates never weakened, manual and
version in lockstep, PR to develop, owner merges. Repeat.
