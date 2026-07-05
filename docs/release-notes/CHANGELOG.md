# Changelog — FKMed

All notable changes to FKMed. Versions follow SemVer with the lockstep rule
(`backend/pom.xml` = OpenApiConfig = this file — DECISIONS-BASELINE §0015). Tags are cut
by the owner only (§0023). Docs-only slices do not bump the version.

## [Unreleased]

*(nothing yet)*

## [0.5.0] — 2026-07-05

Home (SPEC-0005) — Phase 1, slice 1.4. **Closes Phase 1.**

### Added

- **Home** (post-login landing): beneficiary card reflecting the **active beneficiary** (updates on
  selector switch — the phase's headline journey); "Acesso Rápido" quick-access carousel (9 shortcuts;
  not-yet-delivered modules disabled "em breve"; Reconhecimento Facial info dialog); operator **banner**
  carousel (6 s rotation, pause on hover/focus) and **notices** accordion (single-open,
  severity-distinct) — all content server-side filtered by `GET /api/content/home` (new
  `domain.content` module, Flyway V8, ADR-0006). Navigation to not-yet-delivered modules is "em breve"
  (SPEC-0005 AC2/AC6 deferred to Phases 2/5).
- Post-login landing is now the **Home**; "Início" navigation entry added.

## [0.4.0] — 2026-07-04

Beneficiary context & family-scope authorization + identity hardening (SPEC-0003) — Phase 1,
slice 1.3.

### Added

- **Active-beneficiary context** (`domain.plan`): `GET /api/context/accessible-beneficiaries`
  (selector source — a titular sees self + dependents, a dependent only self) and
  `GET /api/context/beneficiaries/{id}` (scope-checked card summary). Out-of-scope requests answer
  `404 context.beneficiary-not-accessible` without revealing the beneficiary exists (SPEC-0003
  BR1-BR5, BR8). Server-side enforcement in the new `BeneficiaryAccess` facade (DL-0004); the
  client's active beneficiary is convenience only.
- **Active-beneficiary selector** in the shell header (Angular): avatar, first name and role
  ("MARIA · Responsável"); switching updates the active context (BR5).

### Hardened

- **Optimistic lock on `user_account`** (Flyway V6, `@Version`): concurrent failed-login
  increments no longer lost-update each other — each attempt runs in its own transaction with a
  bounded retry, and a residual conflict is translated to `ConcurrentAccountUpdateException`
  (`409 auth.concurrent-update`) instead of leaking the raw framework exception (débito técnico A,
  DL-0005; regression `ConcurrentFailedLoginIT`).

### Tests / tooling

- **Account-security E2E** now runs on a dedicated disposable account (`seguranca-e2e@fkmed.local`,
  Flyway V7 — dev-only, refused in prod by `ProdReadinessValidator`) instead of mutating MARIA's
  canonical account (débito técnico B).

## [0.3.0] — 2026-07-04

Account security — lockout, recovery, password change, sessions, Segurança screen (SPEC-0002) —
Phase 1, slice 1.2. *(Recorded retroactively: slice 1.2 merged (PR #9) without the lockstep bump;
the version was reconciled in slice 1.3.)*

### Added

- **Lockout** (BR8): 5 consecutive failed logins lock the account for 15 min; attempts during the
  lock are refused (DL-0002).
- **Password recovery** (BR10): 30-min single-use reset link; a successful reset terminates all
  other active sessions. **Authenticated password change** (BR11) requiring the current password.
- **Session lifecycle** (BR12): "manter conectado" (7-day) vs session-only (30-min idle), with a
  "Sua sessão expirou" return-route notice.
- **Segurança screen**: change password, read-only login e-mail, mobile-biometrics info card.
- Recovery/change events audited (BR14); recovery/change e-mails via the identity mail seam
  (DL-0003).

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
