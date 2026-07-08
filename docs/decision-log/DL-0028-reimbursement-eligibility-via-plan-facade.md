# DL-0028 - Reimbursement eligibility via the plan facade

- **Phase/slice:** Phase 6 / 6.1 Reimbursement request
- **Spec(s):** SPEC-0015 (BR1, AC8)
- **Related ADR:** ADR-0022
- **Date:** 2026-07-08
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

BR1 gates the whole reimbursement module on the caller's plan entitlement, but the reimbursement
module must not read plan entities or repositories across the module boundary.

## Decision

Expose a small method on `BeneficiaryAccess`: `reimbursementEligible(String beneficiaryCard)`.
It returns false when the caller card is missing or unknown, preserving fail-closed behavior.

## Justification

`domain.plan` owns the beneficiary-plan relationship and already exposes family-scope facades to
other modules. A boolean eligibility facade is narrower than leaking a plan DTO or repository.

## Alternatives discarded

- Query `plan` from `domain.reimbursement` - rejected by module-boundary rules.
- Duplicate plan data into reimbursement - rejected as unnecessary denormalization.
- Let only the frontend gate eligibility - rejected because backend is the final authority.

## Impact

`BeneficiaryAccess` gains one public read method; every reimbursement read/write path checks it.

## How to revert

Replace the boolean method with a richer entitlement view if future reimbursement slices need more
plan parameters at runtime.
