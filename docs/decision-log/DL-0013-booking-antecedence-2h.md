# DL-0013 — Minimum booking antecedence = 2 hours (SPEC-0009 OQ1)

- **Phase/slice:** Phase 3 · Appointments (SPEC-0009)
- **Spec(s):** SPEC-0009 (BR5; Open Question OQ1)
- **Related ADR:** ADR-0012 (domain.appointment)
- **Date:** 2026-07-05
- **Status:** OWNER-DECIDED (confirmed via AskUserQuestion)
- **Confidence:** High
- **Reversibility:** Cheap

## Gap

SPEC-0009 OQ1: the minimum antecedence for booking a slot (how close to the start time a booking is
still allowed).

## Decision

**2 hours** (owner-decided). Availability filtering (`GET /api/appointments/availability`) does not
offer slots that start in less than 2 h from now; the horizon is still today→+30 days (BR5).

## Justification

Owner choice; a reasonable balance between last-minute booking and operational lead time.

## Impact / How to revert

A single threshold in the availability query; change the value (config/constant) to adjust.
