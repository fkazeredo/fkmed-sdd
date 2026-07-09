# Contributing

## Branch model (DECISIONS-BASELINE §0023)

Pragmatic Git Flow: `main` and `develop` are protected. They change only via reviewed Pull
Request. Work happens on `feature/*` / `bugfix/*` branches; PRs target `develop`; `main` is
updated only via release PR (`develop` -> `main`), and a human cuts the tag. AI agents push
feature branches and open PRs as the automatic close of an authorized green slice; no second owner
prompt is required. `local-only` or `no PR` suppresses publication; `draft` opens a Draft PR.
Agents never merge, tag or force-push.

## Commits

Use Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`). For delivered code/slice
groups, `feat` drives MINOR and fix-only drives PATCH; docs-only does not bump. Work already grouped
under the current unreleased version does not create another bump (baseline §0015).

## Quality gates

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
cd frontend && npm run e2e:up && npm run e2e && npm run e2e:down
cd backend && ./mvnw -Pmutation org.pitest:pitest-maven:mutationCoverage
```

Run gates proportionally:

- Backend gate when backend/domain/API/persistence changed.
- Frontend gate when frontend changed.
- E2E when a user journey changed.
- PIT/mutation for money or critical domain rules when useful.

Never weaken, skip or delete a gate to make code pass (CLAUDE.md invariant 5). Snapshots are
regenerated only deliberately: `-Dopenapi.snapshot.write=true` /
`-Dmodulith.diagram.write=true`.

## Spec-driven development

Read the spec in `docs/specs/` before relevant work; update it in the same PR when the
requirement changed; never invent business rules - ask the owner (CLAUDE.md invariants 3/4).

The normal workflow is FKMed Lean SDD: the main Claude conversation executes the slice;
agents are used for spec/design (`architect`), fresh review (`reviewer`) or risk-based QA
(`qa`).

## Secrets

Never commit secrets (see `SECURITY.md`). Install the pre-commit hook:

```bash
pip install pre-commit && pre-commit install
```
