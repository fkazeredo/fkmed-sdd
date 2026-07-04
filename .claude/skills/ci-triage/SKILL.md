---
description: >
  Diagnoses red checks on a PR/branch in GitHub Actions: collects the right logs, classifies
  the failure (action configuration vs flaky test vs real regression vs snapshot drift) and
  proposes the fix — with a regression test when applicable. Use when CI failed, PR checks
  are red, or Actions broke. Keywords: CI, checks, Actions, pipeline vermelho, build failure.
argument-hint: "[PR-number or branch]"
allowed-tools: Read, Grep, Glob, Bash
---

# /ci-triage — diagnose red CI

All communication is in **pt-BR**. Report each finding immediately, not only at the end.

## 1. Collect

```bash
gh pr checks <N>                      # overview (or: gh run list --branch <branch>)
gh run view <run-id> --log-failed     # ONLY the logs of what failed
```

Read the FIRST failure's log in each job — the rest is usually cascade.

## 2. Classify into one of 4 families

**(a) Action/workflow configuration** — the job fails BEFORE running any project code (error
on the first line, message from the action itself). Real example: gitleaks-action requiring
`GITHUB_TOKEN` on pull_request events — it looks like a "security failure", it is config;
**not a leak, not a bug**. Fix: in `.github/workflows/*.yml`.

**(b) Flaky / test isolation** — green on local Windows, red on the Linux runner. This
codebase's classic signature: an **absolute-count** assertion off-by-N in an integration test
⇒ residue from ANOTHER class on the **shared singleton Postgres** (all integration classes
share one container + cached Spring context). Fix: **`@BeforeEach`** cleanup (besides
`@AfterEach`) on the asserted tables. Not a product bug — confirm the behavioral assertions
pass.

**(c) Real regression** — the code is wrong. Fix the code **+ a regression test that fails
before and passes after, at EVERY reachable layer** (invariant 8).

**(d) Gate drift** — the contract/topology changed on purpose and the committed snapshot fell
behind. Fix: regenerate and commit:
```bash
cd backend && ./mvnw verify -Dopenapi.snapshot.write=true      # docs/api/openapi.json
cd backend && ./mvnw verify -Dmodulith.diagram.write=true      # modules.puml
```

## 3. Faithful local repro (when the log is not enough)

CI runs on **Linux**; a faithful repro = a Linux container with a **CLEAN checkout**:

```bash
git worktree add /tmp/ci-repro <branch>    # or a shallow clone — NEVER the current working tree
docker run --rm -v /tmp/ci-repro:/workspace -w /workspace/backend \
  -v /var/run/docker.sock:/var/run/docker.sock eclipse-temurin:21-jdk ./mvnw verify
```

**Real trap (cost hours):** mounting a Windows working tree with a compiled `target/` inside
a Linux container produces FALSE failures — e.g. JaCoCo "class not found" for synthetic
classes (`Foo$1.class`). If an error only shows in your repro and not in CI, suspect your
setup before suspecting the code.

## 4. Closing

- The fix goes on the **SAME branch as the PR** — the push re-runs the checks automatically.
- **Never disable, weaken or skip a gate** to pass (invariant 5). If a gate seems wrong,
  propose the change to the owner with a justification — do not bypass it.
- Report: failure family, root cause, fix applied, regression test (or why not applicable),
  and the run link for verification.
