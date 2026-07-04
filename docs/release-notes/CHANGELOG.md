# Changelog — FKMed

All notable changes to FKMed. Versions follow SemVer with the lockstep rule
(`backend/pom.xml` = OpenApiConfig = this file — DECISIONS-BASELINE §0015). Tags are cut
by the owner only (§0023). Docs-only slices do not bump the version.

## [Unreleased]

*(nothing yet)*

## [0.2.0] — 2026-07-04

First access + real login + audit foundation (SPEC-0002, SPEC-0003 foundation) — Phase 1,
slice 1.1.

### Added

- **Identity module** (`domain.identity`, Flyway V3): self-service **first access** — identity
  verification (CPF + card + birth date), account creation (login e-mail + password), acceptance
  of the current Terms/Privacy versions, and a 24 h e-mail verification link. Endpoints
  `POST /api/auth/first-access/{verify,complete}` and `POST /api/auth/verification/{confirm,resend}`.
- **Real login**: the embedded Authorization Server now authenticates against `user_account`
  (BCrypt) — `ACTIVE` required; unverified accounts are refused with a resend affordance;
  credential failures stay neutral (no user enumeration). The token carries the `beneficiary_card`
  claim of the linked beneficiary.
- **Audit foundation** (`domain.audit`, Flyway V4): an append-only, immutable audit trail with a
  reusable recording contract (`AuditRecorder`), event-type `*Codes`, personal-data masking, and a
  **12-month retention** sweep (owner decision — SPEC-0003 BR10). Identity events recorded: account
  created, e-mail verified, login success/failure, logout.
- **E-mail seam** (ADR-0004): a `MailSender` port + SMTP adapter with a logging fallback; the
  identity module publishes `AccountCreated` (AFTER_COMMIT) and an infra listener sends the
  verification link. **Mailpit** added to the dev and E2E compose stacks as the SMTP catcher.
- Angular **Primeiro acesso** wizard (3 steps, client-side policy + show/hide, pt-BR) and
  **Verificação de e-mail** landing (confirm + neutral resend); public routes outside the shell.

### Changed

- Retired the in-memory dev-login seam (`DevLoginConfig`/`NoAccountsConfig`); MARIA's seeded
  `ACTIVE` account (`maria@fkmed.local` / `maria12345`, dev-only) replaces it. `ProdReadinessValidator`
  now also refuses to boot when the dev account or an empty registration-token secret is present.
- SPEC-0002 and SPEC-0003 marked **Approved**; SPEC-0003 OQ1 resolved to 12-month retention.

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
