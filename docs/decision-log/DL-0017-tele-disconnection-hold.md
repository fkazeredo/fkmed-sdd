# DL-0017 — Queue disconnection hold = 2 minutes (SPEC-0010 OQ2)

- **Phase/slice:** Phase 4 · Telemedicine (SPEC-0010)
- **Spec(s):** SPEC-0010 (OQ2, BR5/BR6)
- **Related ADR:** ADR-0014 (domain.telemedicine)
- **Date:** 2026-07-06
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0010 OQ2 asks how long a queued session survives a client disconnection before it may expire,
so a transient network blip does not cost the beneficiary their place in line.

## Decision

A session stays `EM_FILA` **holding its position for up to 2 minutes** of disconnection (the spec's
proposed default). The SSE stream reconnecting within that window resumes the same session/position;
beyond it the ordinary lifecycle applies (no-show on turn, or manual leave). A single parameter.

## Justification

2 minutes covers common blips without letting an absent user hold a slot indefinitely (fairness).
It is a tuning parameter, not a structural choice — cheap to change.

## Alternatives discarded

- Drop on first disconnection — rejected: punishes transient blips, bad UX.
- Indefinite hold — rejected: unfair to the rest of the queue.

## Impact / How to revert

One duration constant in `domain.telemedicine`. Change the value; no schema/contract impact.
