# ADR 0011: Module map revision — `domain.network` (provider network search)

## Status

Proposed

## Context

SPEC-0008 introduces the accredited-provider network search: a real geography registry, a provider
registry with specialties/seals, and a funnel + name search filtered by the plan's coverage. This is
a new bounded context (operator-loaded network data + search) with its own tables and rules (BR1–14)
— it does not belong to `domain.plan` (the beneficiary/contract), `domain.identity`, `domain.card`,
`domain.notification`, `domain.content` or `domain.audit`. ADR-0001's "grow the map one spec at a
time" policy is triggered.

## Decision

We will add **`domain.network`** as the 8th verified Modulith module (`explicitly-annotated`,
consistent with the map), owning: the geography registry **`municipality`** (IBGE code, name, `uf` FK
→ the existing `uf_registry`) seeded with the full IBGE list (DL-0014); the registries
`service_type`, `specialty`, `seal` (registry data, §0019); and `provider`(+`provider_specialty`/
`provider_seal`) referencing `municipality` + a free-text `neighborhood`. Its public API is
read-only `NetworkController` (`/api/network/*`). Funnel lists are **derived from active providers
within the plan's coverage** (BR3/BR4: `plan.coverage`/`plan.coverage_uf`, DL-0014). The **specialty
registry is shared with SPEC-0009** — exposed via a public facade/validator that `domain.appointment`
consumes (a one-directional `appointment → network` dependency, no cycle). Registries follow the
**actual dedicated-table precedent** (`uf_registry`, `notification_event_type`), **not** the literal
generic-`(type,code,label,active,sort_order)` table described in DECISIONS-BASELINE §0019 — the
baseline text and the codebase's practice diverge; we follow practice and flag it here.

## Consequences

- **Positive:** a normalized, real geography + provider foundation; read-only, no events, low
  coupling; the specialty registry is reused by appointments without duplication.
- **Negative:** an 8th module raises the `ModularityTest`/diagram surface; the full IBGE municipality
  seed (~5,570) is large and mostly unexercised until the network grows beyond RJ (DL-0014).

## Alternatives Considered

- **Fold into `domain.plan`** — rejected: network data is a different owner/lifecycle (operator
  network vs. ANS contract), would make `domain.plan` a dumping ground.
- **Generic §0019 single-table registry** — rejected: diverges from the established
  dedicated-table practice; a discriminator table adds indirection for no gain here.

## Revision Triggers

- Provider network gains write/CMS operations (today migration-only).
- A second module needs the geography registry → consider promoting it to a shared location.

## References

SPEC-0008 · ADR-0001 (module map, revised here) · ADR-0003 (single-tenant) · DL-0012 (seals) ·
DL-0014 (geography + coverage) · DECISIONS-BASELINE §0019 (registry — note the divergence) ·
`docs/architecture/modules-and-apis.md` · diagram `docs/architecture-diagrams/modules.puml`.
