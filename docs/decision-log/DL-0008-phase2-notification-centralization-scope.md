# DL-0008 — Phase-2 notification centralization scope

- **Phase/slice:** Phase 2 · Notifications (SPEC-0004)
- **Spec(s):** SPEC-0004 (Goal, BR6/BR7), SPEC-0006 (Events: ContactDataChanged)
- **Related ADR:** ADR-0008 (domain.notification module)
- **Date:** 2026-07-05
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0004 "centralizes the mechanism", but Phases 0–1 already send account e-mails directly
through the identity `*EmailListener`s (verification, recovery, password-changed). The spec does
not say how much of that to migrate into the new module in Phase 2 without regressing the
green Phase-1 e-mail flows.

## Decision

Minimal centralization that satisfies every Phase-2 acceptance criterion:

1. `domain.notification` creates the **in-app** item for events that already exist
   (`PasswordChanged`, account-lockout). Those catalog types are seeded `email_default=false`,
   so the module does **not** e-mail them — the existing identity `*EmailListener`s keep sending
   those e-mails unchanged (no double send, no Phase-1 regression).
2. The new `account.contact-changed` type (from SPEC-0006) is handled **end-to-end** by the
   module: in-app **and** e-mail to the **old and new** addresses (SPEC-0006 Events). Producer
   wiring (`ContactDataChanged` listener) is added at integration, after SPEC-0006 lands.
3. Full migration of the transactional account e-mails (verification, recovery) into the module
   is **deferred** — no acceptance criterion requires it.

The e-mail policy is data-driven by the registry `email_default` flag per type.

## Justification

Rule Zero: the smallest change that meets all Phase-2 ACs while keeping Phase-1 e-mail behavior
green. Migrating every account e-mail now would enlarge the blast radius and risk regressions for
no AC benefit. The registry flag makes the policy explicit and reversible per type.

## Alternatives discarded

- **Migrate all account e-mails into the module now** — rejected: larger blast radius, risks
  Phase-1 regressions, unrequired by any AC.
- **Module also e-mails password-changed** — rejected: double e-mail (the identity listener
  already sends it).

## Impact

- Files: catalog seed `email_default` flags; a listener turning `PasswordChanged` + lockout into
  in-app items; the `ContactDataChanged` end-to-end wiring added at integration.
- The existing `infra.email.*EmailListener` classes are **not** modified.

## How to revert

Flip `email_default` and/or move a transactional e-mail into the module type-by-type when a
deliberate consolidation slice is scheduled.
