# DL-0006 — Notification recipient for a beneficiary without an account (SPEC-0004 OQ1)

- **Phase/slice:** Phase 2 · Notifications (SPEC-0004)
- **Spec(s):** SPEC-0004 (BR8; Open Question OQ1)
- **Related ADR:** ADR-0008 (domain.notification module)
- **Date:** 2026-07-05
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0004 BR8 routes business events to the target **beneficiary's** account, but some
beneficiaries (minor dependents) have **no account of their own**. OQ1 leaves open who
receives the notification in that case — a wrong guess either spams the titular or silently
drops state changes the family must act on.

## Decision

Deliver to the **titular's account** (the legal responsible for the family). The delivery
resolver defaults a beneficiary-without-account target to that beneficiary's titular. This is
**seeded for the future**: in Phase 2 the only wired events are account/security events that
target a user account directly, so no beneficiary-without-account path is exercised by any
Phase-2 acceptance criterion — the default lands now so the first beneficiary-targeted
business event (reimbursement/guide, Phases 5/6) inherits a defined behavior.

## Justification

The titular already holds family scope over dependents (SPEC-0003 family model); routing a
minor's state change to the responsible titular is the only delivery that keeps the family
able to act. Silently dropping loses the event; requiring the minor to self-serve contradicts
the product (minors don't have accounts).

## Alternatives discarded

- **Drop the event silently** — rejected: loses state changes with real-world consequences.
- **Require the beneficiary to have an account** — rejected: minors have none by design.

## Impact

- Specs: SPEC-0004 OQ1 resolved.
- Files: the notification delivery resolver's default recipient (no Phase-2 code path exercises
  it yet); catalog seeding supports future beneficiary-targeted types.
- Migrations: none beyond the catalog seed.

## How to revert

Change the resolver default when a real per-beneficiary delivery policy is defined by the first
beneficiary-targeted business spec; revisit alongside that slice.
