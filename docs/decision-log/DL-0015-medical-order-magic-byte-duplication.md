# DL-0015 — Duplicate the magic-byte upload check in domain.appointment (SPEC-0009)

- **Phase/slice:** Phase 3 · Appointments (SPEC-0009)
- **Spec(s):** SPEC-0009 (BR4; §Validation)
- **Related ADR:** ADR-0012 (domain.appointment)
- **Date:** 2026-07-05
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0009's medical-order upload (JPG/PNG/**PDF** ≤ 5 MB, content-validated) reuses SPEC-0006's
magic-byte validation — but `ImageContent` (the detector) is **package-private** in `domain.plan`
and the new `appointment` module cannot import it under the module-boundary rule (§0016/ArchUnit).

## Decision

**Duplicate** the small magic-byte check (~15 lines) inside `domain.appointment` (its own
`MedicalOrderContent`/attachment value object), adding the **PDF signature** (`%PDF` = `0x25 0x50
0x44 0x46`) alongside JPEG/PNG, plus the size guard and the appointment-scoped exceptions
(`appointment.attachment-invalid`/`appointment.attachment-required`). Do **not** relocate/promote
`ImageContent` to a shared module.

## Justification

A ~15-line, stable detector duplicated across a module boundary is cheaper and clearer than
creating a shared util module or widening `domain.plan`'s exports for a second consumer (Rule Zero
+ §0016). If a third consumer appears, extracting a shared `infra`/util detector becomes worth an
ADR.

## Alternatives discarded

- Promote `ImageContent` to a shared location — rejected now (premature sharing for two consumers).
- Import across the boundary — rejected (violates §0016/ArchUnit).

## Impact / How to revert

New small VO in `domain.appointment`. Revert = extract a shared detector when a 3rd consumer lands.
