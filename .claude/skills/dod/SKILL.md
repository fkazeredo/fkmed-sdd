---
description: >
  Closes the slice: runs all gates (backend verify, frontend lint/test/build, E2E when
  applicable), walks the Definition of Done from CLAUDE.md and the TUTORIAL, requires
  manual/changelog/version up to date, records the ROADMAP-STATUS line and finishes with the
  feature-branch push + PR to develop (DECISIONS-BASELINE §0023). Use when the slice looks ready or when
  asked to close the slice / run the DoD. Keywords: DoD, definition of done, fechar fatia,
  gates, open PR.
argument-hint: "[slice-name]"
---

# /dod — close the slice

All communication is in **pt-BR**. Announce the expected duration of slow blocks (verify
~minutes, E2E ~minutes) BEFORE running them. **Never hide a failed command.**

## 1. Gates (in order, no skipping)

```bash
cd backend && ./mvnw verify        # Spotless, Checkstyle, JaCoCo, ArchUnit, Modulith+diagram,
                                   # OpenAPI snapshot drift, jqwik
cd frontend && npm run lint && npm test && npm run build
```

- **E2E** when the slice touches a user flow: `cd frontend && npm run e2e:up && npm run e2e`
  (+ `npm run e2e:down` at the end).
- **A red gate ⇒ fix the CODE, never the gate** (invariant 5). Do not proceed to the PR with
  any red gate.

## 2. Definition of Done (read from the living sources — no local copy)

Walk it item by item, reporting in pt-BR:

- `CLAUDE.md` §Definition of Done (the full list).
- `docs/TUTORIAL.md` §3, step-6 checklist.

Checks that tend to slip — verify explicitly:

- Bug fixed in the slice ⇒ **regression test at EVERY reachable layer** (invariant 8); a
  skipped layer requires an explicit stated reason.
- New user-facing text ⇒ i18n in every product locale bundle (full parity + fallback).
- Multilingual product: all language faces of touched artifacts in sync (MANUAL, README,
  CHANGELOG) — none may lag.
- Requirement changed during the slice ⇒ spec updated.
- No orphan TODO/FIXME, no commented-out code, no incomplete implementation (invariant 6).

## 3. Satellites

- **Code changed** ⇒ version bumped? If not: `/release`. **Docs-only** ⇒ no bump (state it).
- **User-visible change** ⇒ manual up to date? If not: `/manual`.
- **Execution-log line** in `docs/ROADMAP-STATUS.md`, following the convention declared in
  the file's own header (America/Sao_Paulo date, outcome, tests, version, DLs).

## 4. Git closing (DECISIONS-BASELINE §0023)

- **Conventional Commits** (small, one purpose per commit).
- `git push -u origin feature/<slice>` and `gh pr create --base develop` — this is the normal
  end of the slice. **NEVER** merge, tag or force-push (`settings.json` enforces it; do not
  work around a denial — explain and ask the owner).
- PR checks red afterwards? ⇒ `/ci-triage`.
- Before the PR, run the **architect's review checklist** over the diff (see
  `.claude/agents/architect.md` §Review function) — with a fresh-eyes pass when the work was
  self-directed. Recommended, not mandatory.

## 5. Final report

In the `CLAUDE.md` §"Final response after implementation" format: files, behavior,
specs/ADRs, tests, migrations, contracts, commands run, verification, risks, pending items.
