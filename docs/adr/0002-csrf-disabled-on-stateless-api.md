# ADR 0002: CSRF protection disabled on the stateless JWT `/api/**` chain

## Status

Proposed

## Context

CodeQL (`java/spring-disabled-csrf-protection`, high severity) flags the `csrf.disable()`
call on the `/api/**` security filter chain in `SecurityConfig`. The finding blocks the
CodeQL check on PRs. The question it raises is real and deserves a recorded answer rather
than a silent suppression: is disabling CSRF on this chain safe?

The `/api/**` chain is configured `SessionCreationPolicy.STATELESS` and authenticates
exclusively via `oauth2ResourceServer(jwt)` — a Bearer access token carried in the
`Authorization` header by the Angular SPA (OIDC Authorization Code + PKCE). No session
cookie participates in `/api/**` authentication. CSRF attacks depend on the browser
**automatically** attaching an ambient credential (a session cookie) to a forged
cross-site request; a Bearer token in a request header is never attached automatically by
the browser cross-site, so the CSRF attack vector does not exist on this chain. Leaving
CSRF enabled would add no security value and would force the SPA to carry a CSRF token on
every future state-changing `/api/**` call. This is the canonical Spring Security guidance
for stateless token-authenticated APIs.

## Decision

We will keep Spring Security's CSRF protection **disabled on the `/api/**` chain only**,
because that chain is stateless and authenticated solely by Bearer JWT, where CSRF is not
exploitable. CSRF protection **remains enabled** on the session-bearing chains (the
Authorization Server endpoints and the form-login fallback), where it is meaningful. The
CodeQL alert on this specific line is classified a false positive for stateless JWT APIs
and is dismissed with a reference to this ADR; the repository's own `codeql.yml` continues
to scan and report to the Security tab.

## Consequences

- **Positive:** the SPA calls `/api/**` without CSRF-token plumbing; the security model
  stays simple and idiomatic; the decision and its rationale are recorded and reviewable.
- **Negative / risk:** the safety of this choice is **contingent on `/api/**` never
  authenticating via a cookie/session**. If any future endpoint on this chain starts
  relying on a session cookie for authentication, CSRF becomes exploitable and this ADR
  must be revised (see Revision Triggers). A dismissed CodeQL alert also means this exact
  line no longer re-alerts — the guard is this ADR plus code review, not the scanner.

## Alternatives Considered

- **Leave CSRF enabled on `/api/**`** — rejected: it adds no protection for a Bearer-token
  stateless API (no cookie to forge) and imposes CSRF-token handling on every future write
  request from the SPA, complicating the client for zero security gain.
- **Inline/comment-based suppression of the CodeQL rule** — rejected: it hides the reasoning
  at the call site without the structural record the baseline expects for a security
  decision; an ADR + Security-tab dismissal keeps a single, auditable source of truth.
- **Switch `/api/**` to cookie/session auth with CSRF tokens** — rejected: contradicts the
  chosen stateless resource-server architecture (DECISIONS-BASELINE §0018) and the SPA's
  OIDC token flow.

## Revision Triggers

- Any `/api/**` endpoint begins authenticating via a session cookie (not a Bearer token).
- The session-creation policy of the `/api/**` chain changes away from `STATELESS`.
- A CSRF-relevant credential (e.g. a cookie-stored token readable by the browser on
  cross-site requests) is introduced for API calls.

## References

- `backend/src/main/java/com/fkmed/infra/security/SecurityConfig.java` (`apiChain`).
- DECISIONS-BASELINE §0018 (ordered filter chains, stateless resource server).
- SPEC-0001 BR3 (authenticated `/api/**`).
- Spring Security reference — CSRF and stateless authentication.
