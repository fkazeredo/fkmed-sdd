# DL-0029 - No-reimbursement beneficiary fixture for SPEC-0015 AC8

- **Phase/slice:** Phase 6 / 6.1 Reimbursement request
- **Spec(s):** SPEC-0015 (BR1, AC8)
- **Related ADR:** ADR-0022
- **Date:** 2026-07-08
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

AC8 needs a realistic authenticated beneficiary whose plan has no reimbursement right. Existing
seeded beneficiary accounts are entitled to reimbursement.

## Decision

Seed one fictitious dev/test-only titular account on a plan with `reimbursement = false`, using the
same disposable-fixture convention as the E2E accounts created earlier.

## Justification

The negative eligibility journey must be reproducible in integration and E2E tests without mutating
shared production-like Maria/Pedro fixtures.

## Alternatives discarded

- Toggle Maria's plan during tests - rejected because the shared fixture would pollute other tests.
- Mock eligibility in frontend only - rejected because AC8 is a backend authorization behavior too.
- Create the account only inside E2E setup - rejected because API integration tests also need it.

## Impact

Migration V27 seeds one plan, one beneficiary and one user account for the negative eligibility
scenario.

## How to revert

Remove the fixture in a future migration and change tests to create their own isolated account setup.
