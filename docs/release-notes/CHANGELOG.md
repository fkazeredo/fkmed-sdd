# Changelog — FKMed

All notable changes to FKMed. Versions follow SemVer with the lockstep rule
(`backend/pom.xml` = OpenApiConfig = this file — DECISIONS-BASELINE §0015). Tags are cut
by the owner only (§0023). Docs-only slices do not bump the version.

## [Unreleased]

*(nothing yet)*

## [0.8.0] — 2026-07-06

Phase 4 — "Cuidado digital" (SPEC-0010 Telemedicine, SPEC-0011 Clinical Documents, + the
telemedicine slice of SPEC-0018 Operator Simulation). **Closes Phase 4.**

### Added

- **Telemedicine** (SPEC-0010): a **Pronto Atendimento** virtual queue (triage → versioned term →
  queue → room → closure summary) with position/ETA pushed **live over SSE**, a **state-driven room**
  (professional + CRM + running duration, no media — ADR-0015), the single-active-session rule,
  no-show expiry, and **scheduled teleconsultation** built on SPEC-0009 (virtual "Telemedicina" unit +
  modality + a 10-minute join window). New `domain.telemedicine` module (11th) + `/api/tele/*` (V19;
  ADR-0014/0015/0016; DL-0017/0018/0022).
- **Minha Saúde — clinical documents** (SPEC-0011): a read-only hub with 3 categories (exam orders ·
  referrals · prescriptions/sick notes), combined **beneficiary + period** filters, validity badges,
  type-specific detail (incl. the **CID** on sick notes — DL-0020), faithful **PDF** download, and a
  referral **"Agendar consulta"** handoff that opens the SPEC-0009 wizard with the specialty
  pre-selected. New `domain.clinicaldocs` module (10th) + `/api/clinical-documents*` (V18; ADR-0013;
  DL-0019).
- **Operator-simulation — telemedicine slice** (SPEC-0018): a **dev-only, flag-gated, internal-role**
  REST family `/api/sim/tele/*` + `/api/sim/documents` that drives the operator side (start attending,
  close a session **issuing clinical documents atomically**, issue a document) — so the full Pronto
  Atendimento journey (queue → room → closure → prescription in Minha Saúde) is demonstrable
  end-to-end. Absent/404 in production, 403 for beneficiaries, audited, prod fail-fast (V22; ADR-0017;
  DL-0021).

### Changed

- **Cross-spec wiring**: a telemedicine closure issues clinical documents (SPEC-0010 → SPEC-0011);
  `TeleTurnReached`/`TeleSessionClosed`/`ClinicalDocumentIssued` become in-app + e-mail notifications
  through the SPEC-0004 center (V20 seeds the three catalog types, no clinical content in the bodies).
- The Modulith module map grows to **11 modules**; `AppointmentView` gains a `modality`
  (`PRESENCIAL`/`TELEMEDICINA`) field for the tele badge.

### Technical

- First **SSE (push)** surface in the codebase (Spring `SseEmitter`, periodic re-emit; ADR-0016).
- Scheduled teleconsultation reuses `domain.appointment` unchanged (a virtual unit + modality flag;
  DL-0018) — no second scheduling engine.
- Lockstep bump 0.7.0 → 0.8.0 (pom / OpenApiConfig / OpenAPI snapshot / frontend `appVersion`).

## [0.7.0] — 2026-07-05

Phase 3 — "Encontrar atendimento" (SPEC-0008 Provider Network Search, SPEC-0009 Appointments).
**Closes Phase 3.**

### Added

- **Provider network search** (SPEC-0008): a read-only network hub with a guided **funnel**
  (state → municipality → neighborhood → service type → specialty) and a **name search** (≥ 3
  chars), every list derived from the **active** provider base **within the plan's coverage**,
  plus a provider **detail** sheet (address, phone, specialties, qualification **seals**) with
  route/copy actions. New `domain.network` module + `/api/network/*`; a full **IBGE geography
  registry** (27 UFs + ~5,570 municipalities) and a `plan.coverage`/`plan.coverage_uf` coverage
  model (V15; ADR-0011; DL-0012/0014).
- **Appointments** (SPEC-0009): book, cancel and reschedule **consultations and exams** in the
  operator's own units against **real slot capacity under concurrency** (last-seat race resolved
  fail-fast — exactly one wins), a unique **protocol** (`AG-…`), a **medical-order upload** for
  exams (JPG/PNG/PDF ≤ 5 MB, magic-byte validated) and **Meus Agendamentos** (upcoming/history,
  per-beneficiary filter). New `domain.appointment` module + `/api/appointments/*` (V16;
  ADR-0012; DL-0013/0015/0016). Minimum booking antecedence **2 h**; 30-day horizon.
- **Cross-spec wiring**: `AppointmentConfirmed`/`Cancelled`/`Rescheduled` become in-app plus
  e-mail notifications through the SPEC-0004 center, delivered **AFTER_COMMIT** (V17 seeds the
  `appointment.cancelled`/`.rescheduled` catalog types).

### Changed

- The provider **specialty registry** is shared with appointments (a one-directional
  `appointment → network` dependency; no cycle).
- The Modulith module map grows to **9 modules** (adds `domain.network`, `domain.appointment`);
  `modules.puml` regenerated with the `notification → appointment` edge.

### Technical

- Slot capacity guarded by an optimistic `@Version` lock (fail-fast, no retry — ADR-0012),
  proven by a real 2-thread Testcontainers concurrency IT (exactly one of two racers wins).
- Shared **protocol generator** in `domain.plan` (atomic DB sequence, `AG-AAAAMMDD-####`; DL-0016).
- Lockstep bump 0.6.0 → 0.7.0 (pom / OpenApiConfig / OpenAPI snapshot / frontend `appVersion`).

## [0.6.0] — 2026-07-05

Phase 2 — "Minha conta e identificação" (SPEC-0004 Notifications, SPEC-0006 Profile & Account,
SPEC-0007 Digital Card). **Closes Phase 2.**

### Added

- **Notification center** (SPEC-0004): a header **bell** with a live unread counter, a paginated
  notification center (read/unread, deep links, mark-one/mark-all-read) and per-event-type **e-mail
  preferences** (mandatory security types locked). Domain events become in-app notifications and,
  per type, e-mails delivered **AFTER_COMMIT** so a mail outage never rolls back business work. New
  `domain.notification` module + `/api/notifications*` (V10; ADR-0008; DL-0006/0007/0008).
- **Profile & Account** (SPEC-0006): the profile menu (photo, contacts, security shortcuts, legal
  documents, sign out) with the build **product version**; **avatar photo** upload (JPG/PNG ≤ 5 MB,
  content-validated by magic bytes) per beneficiary, propagated app-wide without re-login; **partial
  contact/address editing** (audited); **versioned** Terms of Use / Privacy Notice with a mandatory
  re-acceptance **interception**; sign out. `/api/beneficiaries/{id}/{profile,contacts,photo}` and
  `/api/legal-documents/*` (V11/V12; DL-0011).
- **Digital Card** (SPEC-0007): the visual card + data sheet for the active beneficiary, "Minhas
  Carteirinhas", copy card number, and a **PDF** download (OpenPDF, card-on-A4). CNS shown in full
  only here; viewing a dependent's card is audited. New `domain.card` module + `/api/cards/{id}` and
  `/pdf` (V9; ADR-0007/0010; DL-0009/0010).
- **Cross-spec wiring**: a contact-e-mail change raises the mandatory `account.contact-changed`
  notification — in-app plus a security-notice e-mail to **both the old and the new** address.

### Changed

- **`AuditRetentionJob` moved from `infra` to the `application` layer** — a scheduled job is a
  delivery mechanism (like a REST endpoint or a queue consumer), so it belongs with the delivery
  adapters and calls the `domain.audit` facade (owner decision).

### Security

- **De-sensitized the JWT** (ADR-0009): the beneficiary card number is no longer stored in the
  issued token — it is resolved server-side — resolving a CodeQL "clear-text storage of sensitive
  information" alert. A regression test asserts issued tokens carry no card number.

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
