# ADR 0001: Initial Modulith Module Map — plan module + error kernel

## Status

Proposed

## Context

The walking skeleton (SPEC-0001) needs its first verified module map. The baseline mandates a
modular monolith with Spring Modulith boundaries (DECISIONS-BASELINE §0001) and the
three-layer package layout with `@ApplicationModule` markers in the domain
(baseline §0010/§0012), but the module list itself comes from FKMed's own domain — never
invented empty (Rule Zero, `docs/architecture/modules-and-apis.md`). The skeleton's only real
business capability is the plan/beneficiary contract behind the "Meu Plano" journey; the
system/platform endpoints (health, version) carry no business rules.

## Decision

We will start the verified map with exactly **two Modulith modules**, detected by the
`explicitly-annotated` strategy: **`domain.plan`** (plan + beneficiary aggregate, the
`my-plan` view and its repositories — owner of the `plan`/`beneficiary` tables and the V1
seed) and the **`domain.error` kernel** (`DomainException`). The system/platform capability
(`/api/system/health`, `/api/system/version`) is **not** a business module: it lives as
delivery (`application.api.SystemController`) over centralized infra (`infra.health`), per
baseline §0010. `ApplicationModules.verify()` plus the committed diagram snapshot
(`docs/architecture-diagrams/modules.puml`, drift-gated) keep the map honest; new modules
enter this map only through their owning specs (0002+).

## Consequences

- Positive: no fake bounded contexts; the Modulith gate is real from day one; each future
  spec adds its module deliberately, with the diagram diff visible in review.
- Negative: the first feature specs (identity, notifications, card…) will each need a map
  revision — a deliberate, reviewed cost.
- The `error` kernel is a module dependency shared by every future module; its API surface
  must stay tiny (code + args only, baseline §0011).

## Alternatives Considered

- **Pre-creating the ~15 modules implied by the spec suite** — rejected: empty module trees
  are architecture theater, explicitly forbidden by BOOTSTRAP §5 and Rule Zero.
- **Default Modulith detection (direct subpackages of the app class)** — rejected: with the
  layered layout (`domain`/`application`/`infra`) it would model the layers, not the business
  modules; the parent project proved the explicitly-annotated strategy for this layout.
- **A `system` business module for health/version** — rejected: it owns no business data or
  rules; delivery + infra suffice (baseline §0010).

## Revision Triggers

- Any new spec introducing a business capability (SPEC-0002 identity is the first).
- A cross-module dependency that `verify()` rejects, forcing a boundary redesign.

## References

SPEC-0001 · DECISIONS-BASELINE §0001/§0010/§0012/§0016 ·
`docs/architecture/modules-and-apis.md` · diagram snapshot
`docs/architecture-diagrams/modules.puml`.
