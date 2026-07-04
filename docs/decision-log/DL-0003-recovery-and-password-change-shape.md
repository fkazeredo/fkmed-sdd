# DL-0003 — Recovery / reset / password-change event and audit shape

- **Phase/slice:** Phase 1 · Slice 1.2 (account security — lockout, recovery, change, sessions)
- **Spec(s):** SPEC-0002 (BR9, BR10, BR11, BR14; §Events; §API Contracts; AC5, AC6, AC8)
- **Related ADR:** ADR-0004 (e-mail seam), ADR-0005 (session windows)
- **Date:** 2026-07-04
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0002 fixes the recovery/change behavior but leaves several shapes to implementation
("shapes are finalized at implementation — the OpenAPI snapshot governs"):

1. Which account states may request a password reset, and the neutral response shape.
2. Whether "the new password must differ from the current one" (BR9 last clause) applies to the
   **recovery reset** as well as the **authenticated change**.
3. How the §Events set (`PasswordRecoveryRequested`, `PasswordChanged`, `AccountLocked`) maps to
   concrete in-process events and audit codes now, given SPEC-0004 (delivery) is not yet built.
4. Whether the authenticated change terminates other sessions (BR10's mass-invalidation).

## Decision

1. **Recovery is offered only to ACTIVE (verified) accounts, with a byte-identical neutral
   response** (mirrors BR7 and slice 1.1's `verification/resend`, DL-0001). `POST
   /api/auth/recovery/request` always answers `202 Accepted` with an empty body; a reset token +
   e-mail are produced only when the e-mail maps to an ACTIVE account. Resetting an
   `EMAIL_NOT_VERIFIED` account is pointless (it still cannot log in until verified — its path is
   `verification/resend`), so it is a silent no-op. Token TTL **30 min**, single-use
   (`invalidateOpenTokens` on request, `used_at` on reset); reuse → `auth.reset-link-invalid` 410.
2. **"Differ from current" applies to the authenticated change only, not the reset.** BR11's change
   validates base policy AND that the new password does not match the current hash
   (`PasswordPolicy.validateChange(email, newPassword, currentHash, encoder)` → 422
   `auth.password-policy-violation`). The recovery reset validates base policy only
   (`PasswordPolicy.validate`) — the user proved e-mail ownership and does not know/remember the
   old password, so a differ-from-current check there is user-hostile and unfounded in the spec.
3. **Concrete events/audit now, delivery-agnostic.** `PasswordRecoveryRequested` and
   `PasswordChanged` are published AFTER_COMMIT and consumed by identity-scoped infra e-mail
   listeners (same seam as ADR-0004; SPEC-0004 will centralize). A single `PasswordChanged` event
   and a single `identity.password-changed` audit code serve **both** the reset and the self-change
   flows, distinguished by a masked `flow` detail (`recovery-reset` | `self-change`) plus a
   `sessionsTerminated` count on the reset. `AccountLocked` (spec §Events) is **deferred**: BR14 is
   satisfied by the `identity.account-locked` **audit** record, and SPEC-0002 requires no
   lockout notification, so publishing a consumer-less domain event now would be speculative
   (Rule Zero). It is added when SPEC-0004 introduces a consumer.
4. **The authenticated change does NOT terminate other sessions.** BR10 scopes mass session
   termination to the recovery reset explicitly ("a successful reset MUST terminate all active
   sessions"); BR11 says nothing of the sort. Confirmed by re-reading BR10/BR11: only reset invokes
   the `ActiveSessions` port.

## Justification

Every choice is the smallest correct form that satisfies the BRs without inventing later specs'
behavior: active-only recovery avoids a meaningless verified/unverified conflation and preserves
BR7 neutrality; scoping differ-from-current to the change matches the work-order reading and avoids
punishing a legitimate reset; one event/code with a `flow` detail avoids premature event
proliferation while keeping the audit trail unambiguous; deferring `AccountLocked` avoids a dangling
event; not terminating sessions on self-change is the literal BR10/BR11 division.

## Alternatives discarded

- **Recovery for any account state** — rejected: resets an account that still cannot log in and
  blurs verification vs recovery; no spec benefit.
- **Distinct `password-reset` vs `password-changed` audit codes** — rejected as premature; a `flow`
  detail carries the same information without two codes to keep in sync. Trivially split later.
- **Publishing `AccountLocked` now with no consumer** — rejected: a consumer-less event is
  speculative infra (Rule Zero); the audit record already meets BR14.
- **Terminating other sessions on self-change too** — rejected: contradicts BR10's explicit "reset"
  scoping; if the owner wants change-also-invalidates, it is one added `ActiveSessions` call.

## Impact

- Specs: none changed (shapes were deferred to the snapshot).
- Files: `PasswordResetToken` (+repo), `ResetLinkInvalidException`,
  `CurrentPasswordIncorrectException`, `PasswordRecoveryRequested`, `PasswordChanged`,
  `ActiveSessions` port (+ `infra.security` adapter), `IdentityService`
  (`requestPasswordRecovery`/`resetPassword`/`changePassword`), `PasswordPolicy.validateChange`,
  `RecoveryController` + `PasswordController` + DTOs, two infra e-mail listeners, `messages.properties`,
  `AuditEventTypes`, the OpenAPI snapshot.
- Migrations: V5 `password_reset_token`.

## How to revert

Each choice is localized behind `IdentityService`: broaden recovery by dropping the `isActive`
filter; add differ-from-current to reset by swapping `validate` for `validateChange`; split the
audit code by adding a constant + mapping; publish `AccountLocked` by adding the event + a listener;
terminate sessions on self-change by calling `ActiveSessions` in `changePassword`.
