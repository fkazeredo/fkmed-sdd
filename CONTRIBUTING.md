# Contributing

## Branch model (DECISIONS-BASELINE §0023)

Pragmatic Git Flow: `main` and `develop` are **protected** — they change only via a reviewed
Pull Request (≥1 review + CODEOWNERS, required status checks, linear history, no force-push).
Work happens on `feature/*` / `bugfix/*` branches; PRs target **`develop`**; `main` is
updated only via a release PR (`develop → main`), from which a human cuts the tag (no `v`
prefix — DECISIONS-BASELINE §0015). AI agents push feature branches and open PRs; they never
merge, tag or force-push.

## Commits

Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`…). The commit type drives
the SemVer digit (baseline §0015).

## Quality gates (all must be green before a PR)

```bash
cd backend && ./mvnw verify          # tests + ArchUnit + Modulith + snapshots + coverage
cd backend && ./mvnw -Pmutation org.pitest:pitest-maven:mutationCoverage   # PIT ≥ 60
cd frontend && npm run lint && npm test && npm run build
npm run e2e                          # Playwright, against compose.e2e.yaml only
```

Never weaken, skip or delete a gate to make code pass (CLAUDE.md invariant 5). Snapshots are
regenerated only deliberately: `-Dopenapi.snapshot.write=true` / `-Dmodulith.diagram.write=true`.

## Spec-driven development

Read the spec in `docs/specs/` before relevant work; update it in the same PR when the
requirement changed; never invent business rules — ask the owner (CLAUDE.md invariants 3/4).

## Secrets

Never commit secrets (see `SECURITY.md`). Install the pre-commit hook:
`pip install pre-commit && pre-commit install`.
