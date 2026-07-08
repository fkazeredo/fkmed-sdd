# DL-0030 - Full Phase 6 automatic analysis after submission

- **Date:** 2026-07-08
- **Phase/slice:** Phase 6 reimbursement
- **Related specs:** SPEC-0015, SPEC-0016
- **Confidence:** Medium
- **Cost of change:** Cheap

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

