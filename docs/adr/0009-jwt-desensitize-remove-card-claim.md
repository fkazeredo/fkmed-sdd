# ADR 0009: De-sensitize the JWT — remove the beneficiary card number from token claims

## Status

Accepted

## Context

The embedded Authorization Server stamps a `beneficiary_card` claim (the 9-digit card number)
into every issued JWT (`BeneficiaryCardTokenCustomizer`, SPEC-0001 BR8 dev-seam, kept through
SPEC-0002), read back by `SecurityContextUserProvider` to build the `UserContext` used for
family-scope resolution. The card number is **product-sensitive**: it is masked everywhere except
the digital-card screen and its PDF (SPEC-0003 BR8, SPEC-0007 BR8). A JWT claim is base64 — not
encrypted — so this is **clear-text storage of sensitive data** in a token that can be logged or
cached. CodeQL flags it ("Clear text storage of sensitive information … returned by an access to
beneficiaryId"). It must be fixed without regressing Phase-1 scoping behavior (invariant 5: never
weaken the gate to make the alert pass).

## Decision

We will **stop storing the card number in the token**. `SecurityContextUserProvider` resolves the
beneficiary card **server-side from the authenticated principal** (via
`IdentityAccounts.beneficiaryCardFor(principal)`) when building `UserContext`, whose shape is left
unchanged, and `BeneficiaryCardTokenCustomizer` plus the `TokenClaims.BENEFICIARY_CARD` claim are
removed. A regression test asserts an **issued token contains no card number** (fails before,
passes after); integration-test auth helpers that fabricated the claim seed a real
account/beneficiary instead.

## Consequences

- **Positive:** the sensitive value leaves the token surface entirely (resolves the CodeQL alert
  and shrinks the leak/attack surface); production consumers of `UserContext.beneficiaryCard()`
  are unchanged because the accessor still returns the card, now resolved fresh.
- **Negative:** one extra read to resolve the card per context build (previously computed once at
  token issuance); integration-test authentication helpers must seed a real account/beneficiary
  rather than inject a claim.

## Alternatives Considered

- **Carry a non-sensitive internal `beneficiary_id` claim instead of the card** — viable, but it
  ripples into every consumer that keys off the card and its queries; the server-side resolution
  keeps `UserContext` and all consumers untouched.
- **Keep the claim and suppress the CodeQL alert** — rejected: invariant 5 (never weaken a gate);
  the data really is sensitive.
- **Encrypt the claim** — rejected (Rule Zero): opaque complexity for data that need not be in the
  token at all.

## Revision Triggers

- Per-context card resolution shows up as a latency or DB-load problem → cache it, or move to a
  non-sensitive internal-id claim.

## References

SPEC-0003 BR8 · SPEC-0007 BR8 · SPEC-0001 BR8 (the original dev-seam claim, superseded here) ·
CodeQL "Clear text storage of sensitive information" · `docs/architecture/security.md`.
