# DL-0021 — Only the telemedicine+documents slice of SPEC-0018 lands in Phase 4 (owner decision)

- **Phase/slice:** Phase 4 · Telemedicine (SPEC-0010) × Operator Simulation (SPEC-0018)
- **Spec(s):** SPEC-0018 (BR5 tele + document actions), SPEC-0010 (BR9/BR10)
- **Related ADR:** ADR-0017 (operator-simulation seam)
- **Date:** 2026-07-06
- **Status:** OWNER-DECIDED
- **Confidence:** High
- **Reversibility:** Cheap

## Gap

SPEC-0010's journey (queue advances → room → closure issuing documents) is **driven by the
operator side**, which SPEC-0018 owns. The ROADMAP schedules SPEC-0018 in Phase 5, but the Phase 4
journey/E2E cannot complete without a driver. What scope of SPEC-0018 lands now?

## Decision

**Owner decided (AskUserQuestion): implement only the narrow telemedicine+documents slice of
SPEC-0018 in Phase 4** — `start-attending the next queued session`, `close a session (issuing
documents)`, `issue a document` — behind SPEC-0018's own guard rails (config flag + internal
operator role + dev-seeded credential + audit + prod fail-fast; routes absent/404 when disabled,
403 for beneficiaries). The rest of SPEC-0018 (guides, reimbursements, invoices, previews) stays in
Phase 5 and **absorbs** this slice (same routes/contract, extended).

## Justification

This is the ROADMAP's own logic ("0018 lands with the journey that needs it" — it schedules 0018
with 0012 for guides) applied to telemedicine: the tele slice is what makes the Phase 4 journey and
its full E2E (AC1/AC3/AC4) demonstrable. Kept minimal (Rule Zero) — no guide/reimbursement/invoice
actions ahead of need.

## Alternatives discarded

- Defer all of SPEC-0018 to Phase 5 — rejected by the owner after the consequence was surfaced: no
  driver ⇒ the queue never advances, sessions never close, no documents issue ⇒ the phase's central
  journey and AC1/AC3/AC4 are undemonstrable.
- Build the full SPEC-0018 now — rejected: pulls guide/reimbursement/invoice scope forward with no
  consumer yet.

## Impact / How to revert

Three flag-gated `/api/sim/**` endpoints + the sim infra. When full SPEC-0018 lands in Phase 5 it
extends the same seam; nothing to revert.
