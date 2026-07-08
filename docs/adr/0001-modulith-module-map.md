# ADR 0001: Initial Modulith Module Map — plan module + error kernel

## Status

Accepted

## Context

The walking skeleton (SPEC-0001) needs its first verified module map. The baseline mandates a
modular monolith with Spring Modulith boundaries (DECISIONS-BASELINE §0001) and the
three-layer package layout with `@ApplicationModule` markers in the domain
(baseline §0010/§0012), but the module list itself comes from FKMed's own domain — never
invented empty (Rule Zero, `docs/architecture/modules-and-apis.md`). The skeleton's only real
business capability is the plan/beneficiary contract behind the "Meu Plano" journey; the
system/platform endpoints (health, version) carry no business rules.

## Decision

We will start the verified map with exactly **two Modulith modules**, detected by the
`explicitly-annotated` strategy: **`domain.plan`** (plan + beneficiary aggregate, the
`my-plan` view and its repositories — owner of the `plan`/`beneficiary` tables and the V1
seed) and the **`domain.error` kernel** (`DomainException`). The system/platform capability
(`/api/system/health`, `/api/system/version`) is **not** a business module: it lives as
delivery (`application.api.SystemController`) over centralized infra (`infra.health`), per
baseline §0010. `ApplicationModules.verify()` plus the committed diagram snapshot
(`docs/architecture-diagrams/modules.puml`, drift-gated) keep the map honest; new modules
enter this map only through their owning specs (0002+).

### Phase 1 · Slice 1.1 revision (SPEC-0002, SPEC-0003)

The map grows by **two modules**, both under `com.fkmed.domain`, keeping the layered layout and
the `explicitly-annotated` strategy:

- **`domain.identity`** (SPEC-0002) — user accounts, first-access verification, e-mail
  verification tokens, term acceptances, password policy and the account lifecycle
  (`EMAIL_NOT_VERIFIED → ACTIVE`). Owns `user_account`, `email_verification_token`,
  `term_acceptance` (V3). Depends on `domain.error` and on `domain.plan`'s **public facade**
  (`Beneficiaries`) to match the identity triple and read the beneficiary card — never on the
  plan module's internals. It publishes the `AccountCreated` event; the verification e-mail is
  delivered by an infra listener over the `MailSender` port (ADR-0004), not by a notifications
  module (SPEC-0004 will centralize that).
- **`domain.audit`** (SPEC-0003 foundation) — the append-only audit trail: `AuditRecorder`
  application contract, `AuditEventTypes` `*Codes`, and the 12-month retention sweep. Owns
  `audit_event` (V4). Depends only on `domain.error`; consumed by `domain.identity` and by
  infra security listeners (login/logout).

The verified map is therefore **four modules**: `domain.plan`, `domain.error`,
`domain.identity`, `domain.audit` (asserted by `ModularityTest`, drawn in the drift-gated
diagram). Cross-context references are stored as **id values** (e.g.
`user_account.beneficiary_id` is a plain `UUID`, with a DB-level FK for referential integrity
only — no JPA relationship crosses a module), so `verify()` stays green. The identity → notification
and legal-document seams are deferred to SPEC-0004 / SPEC-0006 respectively (see those specs and
ADR-0004).

## Consequences

- Positive: no fake bounded contexts; the Modulith gate is real from day one; each future
  spec adds its module deliberately, with the diagram diff visible in review.
- Negative: the first feature specs (identity, notifications, card…) will each need a map
  revision — a deliberate, reviewed cost.
- The `error` kernel is a module dependency shared by every future module; its API surface
  must stay tiny (code + args only, baseline §0011).

## Alternatives Considered

- **Pre-creating the ~15 modules implied by the spec suite** — rejected: empty module trees
  are architecture theater, explicitly forbidden by BOOTSTRAP §5 and Rule Zero.
- **Default Modulith detection (direct subpackages of the app class)** — rejected: with the
  layered layout (`domain`/`application`/`infra`) it would model the layers, not the business
  modules; the parent project proved the explicitly-annotated strategy for this layout.
- **A `system` business module for health/version** — rejected: it owns no business data or
  rules; delivery + infra suffice (baseline §0010).

## Revision Triggers

- Any new spec introducing a business capability (SPEC-0002 identity is the first).
- A cross-module dependency that `verify()` rejects, forcing a boundary redesign.

## References

SPEC-0001 · DECISIONS-BASELINE §0001/§0010/§0012/§0016 ·
`docs/architecture/modules-and-apis.md` · diagram snapshot
`docs/architecture-diagrams/modules.puml`.
