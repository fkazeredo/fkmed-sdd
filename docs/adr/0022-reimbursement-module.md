# ADR 0022: Module map revision - `domain.reimbursement` (Reimbursement)

## Status

Proposed

## Context

SPEC-0015 opens Phase 6 and, together with SPEC-0016/SPEC-0017, introduces the full
reimbursement capability: eligibility, adhesion term, expense/provider/bank validation, uploads,
protocol generation, automatic analysis, timeline, pendencies, payment, statement and non-binding
previews. None of the existing modules owns this language. `domain.finance` owns invoices/copay/IR/
settlement, `domain.guides` owns authorization guides and attendance tokens, and `domain.plan` owns
beneficiary/family/plan scope. Reimbursement has its own lifecycle, documents and money-sensitive
calculation rules.

## Decision

We will add `domain.reimbursement` as a verified Spring Modulith module owning reimbursement
registries, adhesion terms, requests, session items, uploaded documents, timeline events, automatic
calculation/analysis, pendency/payment transitions, paid statement reads and reimbursement previews.
It collaborates synchronously only through public facades (`BeneficiaryAccess`, `ProtocolGenerator`,
`AuditRecorder` where applicable) and reacts asynchronously through notification listeners. Upload
magic-byte detection is extracted to the tiny `domain.upload` kernel after the third consumer
(DL-0027). Operator-side approval/payment/preview conclusion remain in `application.sim`, which calls
this module's facade instead of becoming a domain module.

## Consequences

- **Positive:** keeps reimbursement rules, persistence and future lifecycle evolution in one bounded
  context; preserves module boundaries around plan ownership and notifications; avoids folding a
  money-sensitive workflow into the finance module where it would blur billing vs. claims.
- **Negative:** adds another verified module and schema surface; the module owns both request and
  preview persistence, so tests must guard the money calculation, state machine and user-facing
  contracts together.

## Alternatives Considered

- Put reimbursement under `domain.finance` - rejected because finance is about amounts owed by the
  beneficiary to the operator, while reimbursement is a claim paid by the operator to the titular.
- Put reimbursement under `domain.plan` - rejected because plan only answers entitlement and family
  scope; it should not own request documents, provider data or lifecycle.
- Implement only a generic upload/request table - rejected because SPEC-0015 has domain-specific
  validation, protocol, term and notification rules.

## Revision Triggers

- Reimbursement preview (SPEC-0017) evolves into a standalone pricing/calculation engine shared by
  other products.
- Upload storage moves from database bytes to a vault/object-storage adapter.

## References

SPEC-0015, SPEC-0016, SPEC-0017, SPEC-0003 (family scope and protocol), SPEC-0004
(notifications), DECISIONS-BASELINE sections 0001/0012/0019, DL-0025, DL-0026, DL-0027,
DL-0028, DL-0029, migration `V27__reimbursement_request.sql`.
