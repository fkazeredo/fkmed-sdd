# ADR 0006: Module map revision — `domain.content` (Home banners and notices)

## Status

Accepted

## Context

SPEC-0005 (Home) introduces the first operator-managed content capability: banners with an
optional validity window (BR6) and notices (BR7), read through `GET /api/content/home`. This is a
new business capability with its own data ownership (the `banner` and `notice` tables, Flyway V8)
and its own visibility rule (active flag + validity window, evaluated against the current instant)
— it does not belong to `domain.plan` (the health-plan contract and its beneficiaries),
`domain.identity` (accounts/login) or `domain.audit` (the append-only trail). ADR-0001 established
the verified module map and its "grow deliberately, one spec at a time" policy (see its Revision
Triggers): "Any new spec introducing a business capability... enters this map only through their
owning specs." SPEC-0005 is that trigger.

## Decision

We will add **`domain.content`** as a fifth verified Modulith module (`explicitly-annotated`
strategy, consistent with the rest of the map), owning the `banner` and `notice` tables and the
BR9 seed. Its public facade is `HomeContent` (`@Service`, `@Transactional(readOnly = true)`),
consumed only by `application.api.ContentController`. It depends on **no other business module**:
content is generic (not beneficiary-scoped), so there is no cross-module read of `domain.plan` or
any other module's facade. Visibility filtering — `Banner.isVisibleAt(Instant)` (active AND inside
the optional validity window, both bounds inclusive when present) and the notice `active` flag —
runs against an injected `Clock` (the same `infra.platform.TimeConfig` bean every other module
uses), so it is deterministic in tests without a DB round trip per assertion. The verified map is
now **five modules**: `domain.plan`, `domain.error`, `domain.identity`, `domain.audit`,
`domain.content` (asserted by `ModularityTest`, drawn in the drift-gated diagram
`docs/architecture-diagrams/modules.puml`).

## Consequences

- **Positive:** the new module is fully isolated — zero coupling to existing modules keeps
  `ApplicationModules.verify()` trivially green and keeps Home content free to evolve (e.g. a
  future CMS) without touching plan/identity/audit. The validity-window rule lives once, on the
  entity itself, and is unit-tested without Testcontainers.
- **Negative:** content is not yet segmented by beneficiary/profile (explicitly out of scope per
  SPEC-0005 §Out of Scope) — if that requirement lands later, `domain.content` will need to depend
  on `domain.plan`'s facade (or receive an id), reopening this map.

## Alternatives Considered

- **Folding banners/notices into an existing module** (e.g. `domain.plan`, as "operator content
  about the plan") — rejected: banners/notices are not plan data, have a different lifecycle and
  owner (operator content team vs. ANS contract data), and would make `domain.plan` a dumping
  ground unrelated to its actual bounded context.
- **A generic `domain.cms`/`domain.registry` catch-all module for all future operator content
  across specs** — rejected as premature: no second content-owning spec exists yet to justify a
  shared abstraction (Rule Zero); SPEC-0014's antifraud/channel content and SPEC-0004's
  notifications may or may not fit the same shape, and forcing them in now would be architecture
  theater ahead of the actual requirement.

## Revision Triggers

- Banner/notice content becomes beneficiary- or profile-segmented (SPEC-0005 explicitly defers
  this) — `domain.content` would need a dependency on `domain.plan`'s facade.
- A second spec introduces its own operator-managed content with enough shape overlap to justify
  extracting a shared abstraction.
- An operator-facing CMS/authoring capability is added (today content is migration-only).

## References

SPEC-0005 (Home) BR6/BR7/BR9 · ADR-0001 (Initial Modulith Module Map — revised by this ADR) ·
DECISIONS-BASELINE §0012/§0016 · `docs/architecture/modules-and-apis.md` · diagram snapshot
`docs/architecture-diagrams/modules.puml` · `backend/src/main/java/com/fkmed/domain/content/`.
