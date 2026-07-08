# DL-0031 - Phase 6 reimbursement open-question defaults

- **Date:** 2026-07-08
- **Phase/slice:** Phase 6 reimbursement
- **Related specs:** SPEC-0016, SPEC-0017
- **Confidence:** Medium
- **Cost of change:** Cheap

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

