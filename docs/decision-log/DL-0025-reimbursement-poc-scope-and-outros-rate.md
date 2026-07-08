# DL-0025 - Reimbursement POC scope defaults and `OUTROS` table rate (SPEC-0015)

- **Phase/slice:** Phase 6 / 6.1 Reimbursement request
- **Spec(s):** SPEC-0015 (OQ1, OQ2, BR4, persistence changes)
- **Related ADR:** ADR-0022
- **Date:** 2026-07-08
- **Status:** ASSUMED
- **Confidence:** Low
- **Reversibility:** Cheap

## Gap

SPEC-0015 names the `OUTROS` expense type but does not define its reimbursement-table amount.
It also leaves two POC-shaping questions open: multiple expenses under a single protocol and
plan-specific reimbursement multiples.

## Decision

For Slice 6.1, keep one expense per protocol, use `plan_multiple = 1.0` for every seeded table
row, and seed `OUTROS` at the Consulta rate (`120.00`) until the owner defines a distinct value.

## Justification

The spec already proposes "not in the POC" for multi-expense protocols and `1.0` for plan
multiples. The `OUTROS` value is required to keep the registry/catalog complete for BR4 while
avoiding an invented calculation path: it is data in a Flyway seed and can be replaced cheaply.

## Alternatives discarded

- Block `OUTROS` in Slice 6.1 - rejected because BR4 includes it in the expense-type registry.
- Leave `OUTROS` without a table row - rejected because later preview/analysis would need special
  missing-data handling for a valid request type.
- Implement multi-expense protocols now - rejected as explicit scope creep for Slice 6.1.

## Impact

SPEC-0015 moves OQ1/OQ2 into assumed rules. Migration `V27__reimbursement_request.sql` seeds one
`reimbursement_table` row per expense type.

## How to revert

Add a future migration updating the `OUTROS` amount and, if needed, evolve the request model to a
protocol header plus multiple expense items. Changing `plan_multiple` remains a data migration.
