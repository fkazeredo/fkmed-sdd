## What

<!-- What this PR delivers, in business language. Link the spec (docs/specs/NNNN). -->

## Checklist

- [ ] Target branch is `develop` (only release PRs target `main`)
- [ ] No secrets committed (gitleaks green; only enumerated dev defaults — SECURITY.md)
- [ ] `cd backend && ./mvnw verify` green (tests, ArchUnit, Modulith, snapshots, coverage)
- [ ] `cd frontend && npm run lint && npm test && npm run build` green
- [ ] E2E green when a user journey changed (`npm run e2e`, isolated stack)
- [ ] Spec created/updated in the same PR when the requirement changed
- [ ] Flyway migration for any schema change (never edit an applied one)
- [ ] OpenAPI snapshot regenerated deliberately when the contract changed
- [ ] i18n messages added for user-facing text (pt-BR bundle complete)
- [ ] ADR added/updated when architecture changed
- [ ] User manual (`docs/MANUAL.md`) updated for user-visible changes
