# DL-0031 - Phase 6 reimbursement open-question defaults

- **Date:** 2026-07-08
- **Phase/slice:** Phase 6 reimbursement
- **Spec(s):** SPEC-0016, SPEC-0017
- **Related ADR:** ADR-0022
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Context

SPEC-0016 OQ1 asks whether original-document audits can create a post-payment pendency.
SPEC-0017 OQ1 asks whether concluded previews expire. Both questions affect state/copy, but
the specs already propose defaults compatible with the POC scope.

## Decision

Use the proposed defaults for Phase 6:

- no post-`PAGO` pendency state in the POC; post-payment audits are handled through support
  channels outside the system;
- concluded reimbursement previews do not expire; they remain informational, non-binding
  estimates.

## Consequences

- The reimbursement state machine stays exactly as SPEC-0016 BR1 documents it.
- Preview list/detail screens do not need expiry badges or validity windows.
- Both choices are data/UI cheap to revise later if the owner wants stricter audit or
  estimate-validity behavior.

## Alternatives discarded

- Add a post-payment pendency status now - rejected because SPEC-0016 explicitly keeps
  post-payment audits outside the POC state machine.
- Add preview expiration now - rejected because SPEC-0017 treats concluded previews as
  informational history, not binding offers.

## Impact

No extra reimbursement state is added after `PAGO`; preview screens omit validity-window UI. Support
channels remain the route for post-payment audit questions in the POC.

## How to revert

Add a future migration/status and UI badges for post-payment audits or preview expiration. Both
changes are localized to reimbursement state/view models and copy.

