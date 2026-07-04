# DL-0004 — Beneficiary context & family-scope authorization placement and contract

- **Phase/slice:** Phase 1 · Slice 1.3 (beneficiary context & authorization)
- **Spec(s):** SPEC-0003 (BR1-BR5, BR8; §API Contracts; AC1/AC2)
- **Related ADR:** — (ADR-0001 module map unchanged — no new module)
- **Date:** 2026-07-04
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0003 mandates a server-side family-scope authorization contract "consumed by every module" plus
the accessible-beneficiaries selector source, but leaves open: WHERE the scope check and the selector
data live (a new module? which one?), the concrete shape of the two context endpoints, and the error
contract for an out-of-scope request.

## Decision

1. The scope check + selector live in the **plan module** as a new public facade `BeneficiaryAccess`,
   NOT a new Spring Modulith module. The plan module already owns the beneficiary/titular family model
   (`Beneficiary`, `BeneficiaryRepository`), so the family-scope rule (titular → self + dependents;
   dependent → self) is domain logic of that module. No new tables ⇒ no new module boundary is
   justified (Rule Zero; ADR-0001 module map stays {plan, identity, audit, error}).
2. Endpoints (application/api `ContextController`):
   - `GET /api/context/accessible-beneficiaries` → `[{beneficiaryId, firstName, role}]` (selector;
     empty list for an account with no beneficiary card).
   - `GET /api/context/beneficiaries/{beneficiaryId}` → `{beneficiaryId, firstName, fullName, role,
     planName, cardNumber, avatarUrl(null in Phase 1)}`; out of scope → 404.
3. Out-of-scope (or unknown id, or absent card on the summary path) → `BeneficiaryNotAccessibleException`
   → HTTP **404** `context.beneficiary-not-accessible`, deliberately identical to a non-existent
   beneficiary so existence is never revealed (BR2).
4. Business authorization is enforced in the application service, not in `SecurityConfig`
   (docs/architecture/security.md §Business authorization); the client's active-beneficiary choice is
   convenience only (BR3).

## Justification

Placing family-scope logic where the family data already lives keeps the module map minimal and avoids
a cross-module dependency just to answer "who can this account see". 404 (not 403) for out-of-scope is
the literal BR2 requirement and matches the existing `PlanNotFoundException` 404 convention. The
two-endpoint split (list vs per-beneficiary summary) gives the selector a light source and the Home card
(SPEC-0005, next slice) a scoped card source, and it is exactly where AC2 ("PEDRO requests MARIA by id →
not-found") is demonstrable.

## Alternatives discarded

- A dedicated `domain.context` module — rejected (Rule Zero): no own tables/state; would add a 5th
  module + `ModularityTest`/`.puml` churn for logic that is a query over the plan module's family model.
- Enforce scope in `SecurityConfig` / a filter — rejected: ownership/family authorization depends on
  domain state, which security.md routes to the application service; `SecurityConfig` stays coarse
  (`authenticated()`).
- 403 for out-of-scope — rejected: reveals the resource exists (violates BR2); 404 keeps it
  indistinguishable from a non-existent beneficiary.

## Impact

- Specs: none changed (open shapes now fixed; SPEC-0003 §API Contracts already named the
  accessible-beneficiaries path).
- Files: `domain.plan` {`BeneficiaryAccess`, `AccessibleBeneficiary`, `BeneficiarySummary`,
  `BeneficiaryNotAccessibleException`}, `application.api.ContextController`, `infra.web.HttpErrorMapping`,
  `messages.properties`, `docs/api/openapi.json`.
- Migrations: none (reads over existing tables).

## How to revert

Collapse the two endpoints or move the facade to a new module if a real cross-context authorization store
later emerges (which would then warrant an ADR revising the module map). The 404-not-403 choice is
load-bearing for BR2 and should not be flipped without re-checking AC2.
