# DL-0012 — Provider seals as parameterizable qualification badges (SPEC-0008 OQ1)

- **Phase/slice:** Phase 3 · Provider Network Search (SPEC-0008)
- **Spec(s):** SPEC-0008 (BR14; Open Question OQ1)
- **Related ADR:** ADR-0011 (domain.network)
- **Date:** 2026-07-05
- **Status:** ASSUMED
- **Confidence:** High
- **Reversibility:** Cheap

## Gap

SPEC-0008 OQ1: the official meaning of the reference app's "P"/"R" seals is unconfirmed by the
product; only the badge copy is affected.

## Decision

Model seals as **registry data** (`seal`: code, name, **parameterizable description**) — provider
qualification badges — until the product defines the official meaning. The UI shows name +
description on hover/touch (BR12); changing the meaning later is a data edit, no code change.

## Justification

Lowest-risk default (only text), keeps the badge open to the product's future definition without
blocking the network feature. Consistent with the registry-data posture (baseline §0019).

## Alternatives discarded

- Hardcode a specific meaning now — rejected: would invent a business rule the product hasn't set.

## Impact / How to revert

`seal` registry + seed; edit the description when the product defines it (no migration of code).
