# DL-0001 — First-access contract shape (legal acceptance, registration token, resend neutrality)

- **Phase/slice:** Phase 1 · Slice 1.1 (first access + real login + audit foundation)
- **Spec(s):** SPEC-0002 (BR4, BR5, BR7, BR15; §Input/Output Examples, §API Contracts)
- **Related ADR:** ADR-0001 (module map), ADR-0004 (email seam)
- **Date:** 2026-07-04
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0002 fixes the behavior (BRs) but leaves three request/response shapes open — the spec
itself states "shapes are finalized at implementation — the OpenAPI snapshot governs":

1. How the `first-access/complete` request expresses Terms/Privacy acceptance. The I/O example
   sketches `acceptTermsVersion, acceptPrivacyVersion` (client-submitted version strings).
2. The nature and lifetime of the `registrationToken` returned by `first-access/verify`.
3. Whether `verification/resend` reveals account existence.

## Decision

1. **Acceptance = server-authoritative booleans.** The request carries `acceptedTerms` and
   `acceptedPrivacy` (both must be `true`). The server records `term_acceptance` rows against
   the **current** Terms/Privacy versions it holds (config-driven `LegalDocuments`, seeded seam
   for SPEC-0006) plus the acceptance timestamp — fully satisfying BR15 without trusting a
   client-supplied version and without inventing a "stale terms" error (that interception is
   SPEC-0006's).
2. **`registrationToken` = stateless HMAC-SHA256 token** over `beneficiaryId | expiry`, signed
   with a server secret (`app.identity.registration-token-secret`, dev default, prod-validated),
   **TTL 30 min**. No new table (out-of-scope for this slice's migration set); the token only
   bridges verify→complete and is re-validated server-side (existence/age/account checks re-run).
3. **`verification/resend` is neutral** (mirrors BR7): it always answers `202 Accepted`
   regardless of whether the e-mail maps to an unverified account — sending a fresh link only
   when it legitimately applies. This avoids account enumeration on the resend path.

## Justification

The spec delegates shapes to implementation; these choices minimize attack surface (no
client-trusted versions, no enumeration), avoid premature tables/errors owned by later specs
(SPEC-0006), and keep the seam replaceable. Booleans + server-current versions is the smallest
correct form of BR15 (simulation-and-mocking.md: smallest correct seam now).

## Alternatives discarded

- **Client-submitted version strings** — rejected: trusts the client for a legal fact and
  forces a stale-version error path that belongs to SPEC-0006's re-acceptance interception.
- **Persisted registration token table** — rejected: adds schema not in this slice's scope for
  a 30-min handshake already re-validated server-side (Rule Zero).
- **Resend echoing "no such account"** — rejected: leaks account existence (contradicts the
  BR7 neutrality posture applied consistently across auth entry points).

## Impact

- Specs: none changed (shapes were explicitly deferred to the snapshot).
- Files: `first-access/complete` DTO, `IdentityService`, `RegistrationTokenService`,
  `LegalDocuments`, `verification/resend` controller; the committed `docs/api/openapi.json`.
- Migrations: `term_acceptance` stores `document_type` + `version` + `accepted_at`.
- Contracts: OpenAPI snapshot regenerated for the new endpoints.

## How to revert

Swap the boolean fields for version strings and add a stale-version check (small controller +
DTO change) when SPEC-0006 lands its re-acceptance flow; replace the HMAC token with a persisted
one only if a revocation requirement appears. Each is a localized change behind `IdentityService`.
