# DL-0027 - Shared file content-type detector after the third upload consumer

- **Phase/slice:** Phase 6 / 6.1 Reimbursement request
- **Spec(s):** SPEC-0015 (BR8)
- **Related ADR:** ADR-0022
- **Date:** 2026-07-08
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

The project already had upload magic-byte checks in profile photos and appointment medical orders.
SPEC-0015 adds a third consumer and requires JPG/PNG/PDF content validation with different limits.

## Decision

Introduce a tiny `domain.upload` kernel with `FileContentType.detect(byte[])` for JPG, PNG and PDF
magic bytes. Reimbursement uses it with its own size/category rules. Existing profile and
appointment detectors stay untouched in this slice.

## Justification

DL-0015 intentionally duplicated the detector while there were only two consumers. A third consumer
is the planned extraction point, but migrating older modules now would broaden the diff without
improving SPEC-0015 behavior.

## Alternatives discarded

- Keep a third duplicate detector - rejected because the planned extraction threshold was reached.
- Migrate profile and appointment immediately - rejected as unrelated risk and scope creep.
- Put the detector in `infra` - rejected because reimbursement domain rules need it without a
  domain-to-infra dependency.

## Impact

Adds `domain.upload` as a small domain kernel and uses it from `domain.reimbursement`.

## How to revert

Inline the detector back into `domain.reimbursement`, or migrate the older consumers in a later
cleanup once the new kernel is proven.
