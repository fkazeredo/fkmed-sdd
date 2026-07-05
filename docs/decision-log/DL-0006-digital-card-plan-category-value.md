# DL-0006 — Digital-card `planCategory` column and seeded value

- **Phase/slice:** Phase 2 (Digital Card)
- **Spec(s):** SPEC-0007 (BR1, BR2; §API Contracts; AC1)
- **Related ADR:** ADR-0007 (module map revision — `domain.card`)
- **Date:** 2026-07-05
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

The architect's frozen API contract for `GET /api/cards/{beneficiaryId}` names a `planCategory`
field, and SPEC-0007 BR1 requires the card face to show "plan category and name" alongside the
coverage seal. Neither the spec's Business Rules, its Acceptance Criteria (AC1 pins fullName,
cardNumber, cns, ansRegistration and coverage, but not a planCategory value) nor
`docs/specs/README.md` §Canonical reference data fix what "plan category" means or what value
the canonical seeded plan should carry. A column/value had to be chosen to implement migration
`V9__card_registry.sql` and `Plan.java`.

## Decision

1. Add a `category` varchar column on the `plan` table (V9), a plain String like the existing
   `coverage` column (no registry table — same precedent as `coverage`, per the V1 migration's
   own comment that a registry table for these fields "arrives with the spec that manages it",
   baseline §0019).
2. Model it as **distinct from `coverage`**: `coverage` is BR2's seal — the ANS geographic-reach
   value (`ESTADUAL`/`NACIONAL`) — while `category` is the plan's contracting/segmentation
   classification shown next to the plan name on the card face (BR1).
3. Seed value for the canonical plan (`PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP`):
   **"Coletivo por Adesão"** — the standard ANS plan-contracting taxonomy (Individual/Familiar ·
   Coletivo Empresarial · Coletivo por Adesão), read directly off the word "ADESÃO" already
   present in the canonical plan name seeded by SPEC-0001's V1 migration.

## Justification

The value is a read-off of data the product already committed to (the canonical plan name), not
an invention from nothing — "ADESÃO" is literally in the seeded name and is a real, standard ANS
contracting-type term. AC1 does not pin an exact `planCategory` string (unlike cardNumber/CNS/ANS/
coverage, which it does pin verbatim), so the value carries no behavioral weight — it is pure
display data on a POC-fictitious plan, same class of decision as the rest of the V1 canonical seed
(names, CPFs, card numbers), which was itself never owner-authorized field-by-field.

## Alternatives discarded

- **Reuse `coverage` for both the seal and the category label** — rejected: BR1 and BR2 describe
  two different card elements (seal vs. category-and-name line); collapsing them would make BR2's
  "single source of truth" statement vacuous (there would be nothing else to compare it against).
- **Leave `planCategory` always `null`/empty** — rejected: the frozen contract shape must round-trip
  a real value for the frontend to render BR1's card face; an empty field would silently fail BR1
  without surfacing as a test failure.
- **Ask the architect and block** — rejected for this specific gap: the value is low-stakes,
  presentational, and cheap to change later (single seed value, no branching logic reads it), which
  is exactly the class of gap this project's autonomy rules delegate to a recorded DL rather than a
  round-trip.

## Impact

- Specs: none changed (SPEC-0007's contract shape was already fixed by the architect; only the
  concrete value was undetermined — no Business Rule text depends on the specific string).
- Files: `backend/src/main/java/com/fkmed/domain/plan/Plan.java` (new `category` field +
  `create()` parameter), `backend/src/main/java/com/fkmed/domain/plan/CardDetails.java` (new
  record carrying `planCategory`), `backend/src/main/java/com/fkmed/domain/card/CardResponse.java`,
  existing `PlanTest`/`PlanServiceTest`/`BeneficiaryTest` call sites (new constructor parameter).
- Migrations: `V9__card_registry.sql` (`alter table plan add column category ...`).

## How to revert

Change the seeded string in V9 (or a follow-up migration, since V9 will already be applied) and,
if the product later wants a real registry-backed plan-category taxonomy instead of a plain
string, migrate `plan.category` to a registry `code` validated through a `PlanCategories`-style
validator port (baseline §0019) — a follow-up ADR, not a revision of this DL's shape decision.
