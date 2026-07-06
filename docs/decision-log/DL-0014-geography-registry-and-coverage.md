# DL-0014 — Full IBGE geography registry + plan coverage model (SPEC-0008)

- **Phase/slice:** Phase 3 · Provider Network Search (SPEC-0008)
- **Spec(s):** SPEC-0008 (BR1–BR4; §Persistence)
- **Related ADR:** ADR-0011 (domain.network)
- **Date:** 2026-07-05
- **Status:** OWNER-DECIDED (confirmed via AskUserQuestion)
- **Confidence:** High
- **Reversibility:** Cheap (geography is reference data)

## Gap

Two gaps surfaced while planning: (a) the spec seeded only RJ localities as free text on providers;
(b) `plan.coverage` is a free string (`"ESTADUAL"`) that does **not** encode **which** UF(s) the
plan covers, yet BR4 filters the funnel by coverage.

## Decision (owner)

1. **Geography registry.** A dedicated `municipality` registry (IBGE code PK, name, `uf` FK →
   the existing `uf_registry`) seeded with the **full official IBGE list** (27 UFs already in
   `uf_registry`/V11 + ~5,570 municipalities; source: IBGE localidades API). Providers reference a
   **real `municipality`**; `neighborhood` stays **free text** on the provider (no national
   neighborhood dataset). The funnel lists (states/municipalities/neighborhoods offered) are
   **derived from active providers within coverage** (BR3/BR4) over this registry.
2. **Coverage.** Add `plan.coverage_uf` (**nullable**): `coverage='ESTADUAL'` → the covered UF
   code; `coverage='NACIONAL'` → `null` = all UFs. Seeded plan = ESTADUAL / `RJ`.

## Justification

A real, normalized geography foundation (owner's call — "não é difícil conseguir isso") beats
ad-hoc locality strings; providers reference real municipalities; a future NACIONAL plan works
with no schema change. Bairro has no authoritative national dataset, so BR3's "derive from
providers" is the natural source. Registries follow the actual dedicated-table precedent
(`uf_registry`, `notification_event_type`), not §0019's literal generic-table text (see ADR-0011).

## Alternatives discarded

- Single `coverage_uf='RJ'` column (1st-round proposal) — rejected by owner: doesn't model NACIONAL.
- Seed only covered-UF municipalities (RJ) — smaller, but the owner chose the complete IBGE base.
- plan↔UF table — more than needed for the current single-plan/single-tenant deployment (ADR-0003).

## Impact / How to revert

`municipality` registry + IBGE seed (V15); `plan.coverage_uf` column + seed. Trade-off: most
municipalities have no providers until the network grows (unexercised reference data). Revert =
prune the seed / drop the column if coverage is ever modeled differently.
