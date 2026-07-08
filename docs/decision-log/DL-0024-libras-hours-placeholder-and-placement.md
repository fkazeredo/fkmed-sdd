# DL-0024 — Central de Libras operating hours: placeholder value + domain-constant placement (OQ1)

- **Phase/slice:** Phase 5 · 5.3 Atendimento (SPEC-0014)
- **Spec(s):** SPEC-0014 (BR4, OQ1)
- **Related ADR:** ADR-0021 (domain.support)
- **Date:** 2026-07-07
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

OQ1 left the Central de Libras operating hours (and the channel contact numbers) undefined; the
owner authorized fictitious placeholders in the seed, swappable later. Separately, BR4's inside/
outside-hours branch needs a window value the domain can check deterministically in tests.

## Decision

The window (Mon–Fri, 08:00–18:00, `America/Sao_Paulo`) is seeded as **fictitious placeholder**
channel/content values (V25) and, for the domain logic, held as **plain constants** in
`domain.support.LibrasHours` — not a Spring `@ConfigurationProperties` binding. `SupportService`
injects a `Clock` (mirroring `domain.content.HomeContent`) so the inside/outside check is
deterministic in tests. The API's POST response carries the authoritative `hoursStart`/`hoursEnd`
only on the outside-hours outcome (matching the spec's own Input/Output example); the explanatory
page's hours text is static frontend copy — SPEC-0014's API Contracts table has no GET endpoint for
Libras hours, so there is no live value for the explanatory page to fetch anyway.

## Justification

A `@ConfigurationProperties` class would have to live in `infra` (every existing properties class
does), but `SupportService` is a `domain` class — `domain` must not depend on `infra`
(modules-and-apis.md). Introducing that binding just to externalize a placeholder threshold the
owner will replace via a future content migration is unneeded machinery for this scale (Rule Zero).

## Alternatives discarded

- `@ConfigurationProperties` in `infra.support`, injected into an application-layer caller that
  passes plain values into `SupportService` — rejected: adds an indirection layer for a value with
  exactly one caller and no runtime-reload requirement.
- A GET endpoint for Libras hours so the explanatory page always matches the enforced window —
  rejected: not in SPEC-0014's API Contracts table; would be scope the owner did not ask for.

## Impact / How to revert

`LibrasHours` (package-private constants + `isWithin(Instant)`) + the V25 seed's Ouvidoria-style
hours text. Revising the hours later means editing this one class and reseeding via a new
migration — no contract change, since the POST response already threads the values through
`LibrasRequestResult` rather than hardcoding them client-side.
