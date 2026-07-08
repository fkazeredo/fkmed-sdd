# DL-0030 - Full Phase 6 automatic analysis after submission

- **Date:** 2026-07-08
- **Phase/slice:** Phase 6 reimbursement
- **Spec(s):** SPEC-0015, SPEC-0016
- **Related ADR:** ADR-0022
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Context

SPEC-0015, when implemented alone as Slice 6.1, describes a newly submitted request as
`EM_ANALISE`. SPEC-0016 then requires the real documentary/calculation engine to run on
submission and move complete requests to `PROCESSAMENTO`, `PENDENTE_DOCUMENTACAO` or `NEGADO`.
The owner asked to finish the whole Phase 6 in one delivery, not only Slice 6.1.

## Decision

For the full Phase 6 delivery, a complete submission records the initial `EM_ANALISE`
timeline event and immediately advances to `PROCESSAMENTO` after automatic analysis.
The POST response therefore returns the current beneficiary-visible status after the engine
runs. `EM_ANALISE` remains visible in the immutable timeline as the receipt event.

## Consequences

- Aligns the shipped behavior with SPEC-0016 AC1 while preserving the SPEC-0015 receipt
  event.
- Frontend success screens and tests must expect `PROCESSAMENTO` for complete submissions.
- If a future partial slice needs "received only" semantics, it should be introduced as a
  distinct draft/status rather than weakening the Phase 6 state machine.

## Alternatives discarded

- Keep the Slice 6.1-only `EM_ANALISE` response after completing the whole phase - rejected
  because it would hide SPEC-0016's automatic analysis from the shipped contract.
- Remove `EM_ANALISE` from the timeline - rejected because it remains the immutable receipt event.

## Impact

Reimbursement API responses, frontend success screens, integration tests and E2E expectations use
the current post-analysis status. The immutable timeline still records the initial receipt event.

## How to revert

Introduce an explicit draft/received-only status in a future slice and update frontend/API tests to
distinguish draft receipt from completed Phase 6 submission.

