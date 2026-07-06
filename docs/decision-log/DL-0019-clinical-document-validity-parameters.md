# DL-0019 — Clinical-document validity as product parameters (SPEC-0011 BR4)

- **Phase/slice:** Phase 4 · Clinical Documents (SPEC-0011)
- **Spec(s):** SPEC-0011 (BR4/BR5)
- **Related ADR:** ADR-0013 (domain.clinicaldocs)
- **Date:** 2026-07-06
- **Status:** ASSUMED
- **Confidence:** High
- **Reversibility:** Cheap

## Gap

SPEC-0011 BR4 gives default validities (prescription 30d, exam order 90d, referral 90d, sick note
none) and calls them "product parameters, adjustable". How to store/apply them?

## Decision

The `valid_until` is **computed at issue time** from a per-type validity default and **persisted on
the immutable document** (not recomputed on read) — so a later parameter change never mutates an
already-issued document (BR8 immutability). The defaults live as configuration constants/parameters
(prescription 30d, exam order 90d, referral 90d, sick note → `valid_until = null`). Expiry is a
pure read-time comparison of `valid_until` vs today (badge Válido/Expirado, BR5).

## Justification

Persisting `valid_until` keeps documents immutable and audit-faithful; computing at issue keeps the
parameter meaning ("30 days from issue"). Pure read-time expiry needs no job.

## Alternatives discarded

- Recompute `valid_until` on read from the current parameter — rejected: breaks immutability, changes
  historical documents retroactively.
- A validity table per document — rejected: over-engineered for four constants (Rule Zero).

## Impact / How to revert

A `valid_until` column + issue-time computation. Adjust the constants freely; issued documents keep
their stamped validity.
