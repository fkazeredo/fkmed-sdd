# DL-0032 - Upload transport headroom for reimbursement documents

- **Phase/slice:** Hardening / docs reconciliation after Phase 6
- **Spec(s):** SPEC-0015 (BR8)
- **Related ADR:** ADR-0022
- **Date:** 2026-07-08
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0015 allows reimbursement uploads up to 2 MB per file and 20 MB total, but the inherited
Spring multipart request limit was 10 MB and the nginx proxies did not declare a request-body limit.
That could make a valid reimbursement request fail at the transport layer before the domain returns
the documented validation outcome.

## Decision

Set the transport request limit to 25 MB across Spring and nginx (`25MB` in Spring,
`25m` in nginx). Keep the per-file container limit at 10 MB so existing profile and appointment
domain validations still receive oversized single-file requests and return their domain-specific
errors.

## Justification

The 25 MB transport cap gives SPEC-0015's 20 MB business total room for multipart envelope overhead
without turning the API into an unbounded upload endpoint. Per-file business limits remain in the
domain modules because the limits differ by use case.

## Alternatives discarded

- Keep Spring at 10 MB - rejected because valid reimbursement requests near 20 MB could never reach
  the reimbursement service.
- Raise every layer to a much larger value - rejected because the POC has no product case for large
  uploads and should stay conservative.
- Rely only on frontend validation - rejected because the backend and proxies must be authoritative.

## Impact

Updates backend multipart configuration, frontend nginx proxy configuration and production TLS proxy
configuration. Adds a configuration regression test so future changes keep the transport limit in
sync with SPEC-0015.

## How to revert

Change the Spring `max-request-size` and nginx `client_max_body_size` values together. If SPEC-0015's
business total changes, update this decision with a new DL rather than editing it in place.
