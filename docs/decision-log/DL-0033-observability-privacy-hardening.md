# DL-0033 - Observability privacy hardening before a full authorization matrix

- **Phase/slice:** Hardening / docs reconciliation after Phase 6
- **Spec(s):** SPEC-0001 (observability), SPEC-0003 (BR8), SPEC-0015 (Observability Requirements)
- **Related ADR:** ADR-0009
- **Date:** 2026-07-08
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

The architecture docs described a richer production observability/security model than the current
POC implements: a route-role authorization matrix, role-gated actuator loggers, and a user MDC
filter. At the same time, the implemented login event logger still emitted the raw authentication
name, which can be an e-mail address.

## Decision

Apply the low-risk privacy and traceability hardening now: add a request correlation ID filter,
include that ID in access logs and response headers, and mask login event user hints with the
existing audit masking helper. Reconcile the architecture docs to the current POC instead of
claiming `ApiAuthorizationMatrix`, `UserMdcFilter` or role-gated runtime loggers already exist.

Defer a complete endpoint authorization matrix to its own future slice/spec because it changes the
permission model and test surface across the whole API.

## Justification

Correlation IDs and masked login logs are cheap, directly reduce operational/privacy risk and fit
the existing observability posture. A complete authorization matrix is valuable, but implementing it
without a dedicated spec could silently change endpoint behavior and overreach this hardening slice.

## Alternatives discarded

- Implement the full authorization matrix in this slice - rejected as a broad security-contract
  change that needs its own acceptance criteria.
- Update docs only - rejected because raw e-mail hints in authentication logs are an immediate
  privacy hardening opportunity.
- Remove observability docs about the future model entirely - rejected because the model remains a
  useful target; it must be labeled as future work rather than current fact.

## Impact

Adds observability tests/code for correlation IDs and masked login logs. Updates security and
observability architecture docs to distinguish current POC behavior from future production hardening.

## How to revert

Remove the correlation filter and restore the prior log format. A future authorization-matrix slice
can supersede this DL by introducing the matrix, completeness tests and role-gated management
endpoints.
