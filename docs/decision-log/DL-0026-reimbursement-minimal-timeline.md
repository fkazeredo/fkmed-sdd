# DL-0026 - Minimal reimbursement timeline in Slice 6.1 (SPEC-0015)

- **Phase/slice:** Phase 6 / 6.1 Reimbursement request
- **Spec(s):** SPEC-0015 (BR13, AC5)
- **Related ADR:** ADR-0022
- **Date:** 2026-07-08
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0015 requires the first immutable timeline event on submission, while SPEC-0016 will own the
full analysis workflow and later transitions.

## Decision

Create `reimbursement_timeline_event` in Slice 6.1 and write exactly one append-only event when a
request is submitted: status `EM_ANALISE`, with the submission timestamp and a short description.
SPEC-0016 will extend the same table with the remaining transitions.

## Justification

This satisfies BR13/AC5 without bringing the SPEC-0016 state machine into the request slice. The
table is deliberately append-only and cheap to extend.

## Alternatives discarded

- Defer the table to SPEC-0016 - rejected because AC5 requires the initial timeline event now.
- Implement the full state machine now - rejected as explicit out of scope for Slice 6.1.

## Impact

Migration V27 includes `reimbursement_timeline_event`; `ReimbursementRequest.submit` creates the
initial event.

## How to revert

Drop or ignore the table in a future migration and move the initial event to a later lifecycle
module. No API contract currently exposes the timeline in Slice 6.1.
