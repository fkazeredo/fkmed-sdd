# Changelog — FKMed

All notable changes to FKMed. Versions follow SemVer with the lockstep rule
(`backend/pom.xml` = OpenApiConfig = this file — DECISIONS-BASELINE §0015). Tags are cut
by the owner only (§0023). Docs-only slices do not bump the version.

## [Unreleased]

*(nothing yet)*

## [0.1.0] — 2026-07-04

Walking skeleton (SPEC-0001) — the whole architecture path runs end to end.

### Added

- Plan module: `plan`/`beneficiary` baseline (Flyway V1) with the canonical seed (plan ANS
  326305; MARIA titular + PEDRO dependent) and the `GET /api/plan/my-plan` view.
- Public system endpoints: `GET /api/system/health` (app + database status) and
  `GET /api/system/version` (build-info + git commit).
- Embedded Spring Authorization Server (OIDC Code + PKCE, JDBC-persisted state, pt-BR login
  page) with the dev-profile-only login seam bound to MARIA (SPEC-0002 stub).
- Angular shell (top bar, navigation placeholder) and the **Meu Plano** screen, all strings
  from the pt-BR bundle.
- Full quality-gate suite wired into `./mvnw verify` (Spotless, Checkstyle, JaCoCo floors,
  ArchUnit + teeth tests, Modulith verify + diagram snapshot, OpenAPI snapshot, i18n and
  HTTP-mapping completeness) + PIT mutation profile; frontend lint/test/build gates;
  Playwright E2E on the isolated stack; the 5 CI workflows; Docker Compose dev/E2E/prod.
  The jqwik property-test gate (BOOTSTRAP §2 gate 10) is deliberately deferred: this slice
  has no money/critical arithmetic; it arms with the first value-bearing spec (Rule Zero).
