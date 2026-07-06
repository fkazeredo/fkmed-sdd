# DL-0007 — In-app notification retention window (SPEC-0004 OQ2)

- **Phase/slice:** Phase 2 · Notifications (SPEC-0004)
- **Spec(s):** SPEC-0004 (Open Question OQ2)
- **Related ADR:** ADR-0008 (domain.notification module)
- **Date:** 2026-07-05
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0004 OQ2 leaves the in-app retention window open — how long notifications stay visible and
whether they are ever hard-deleted.

## Decision

Keep notifications **12 months visible, no hard delete in the POC**. No retention/cleanup job is
built in Phase 2; all rows persist. A future retention slice may prune rows older than 12 months.

## Justification

Storage is negligible at POC scale and the history has support/audit value; a hard delete is
irreversible and premature before a real retention requirement (and its regulatory basis) exists.
The 12-month visible window is documented now so a later cleanup job has a target.

## Alternatives discarded

- **Hard delete after N days** — rejected: irreversible, premature, no requirement yet.
- **Unbounded forever with no documented window** — rejected: leaves a later cleanup job with no
  agreed boundary.

## Impact

- Specs: SPEC-0004 OQ2 resolved.
- Files/Migrations: none now (no retention job); a future slice owns pruning.

## How to revert

Introduce a retention job with the agreed window when a real retention requirement lands.
