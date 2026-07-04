# DL-0002 — Account-lockout counter and window semantics (BR8 edge cases)

- **Phase/slice:** Phase 1 · Slice 1.2 (account security — lockout, recovery, change, sessions)
- **Spec(s):** SPEC-0002 (BR8, BR7; §Acceptance Criteria AC4)
- **Related ADR:** —
- **Date:** 2026-07-04
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

BR8 fixes the headline behavior ("5 consecutive failures → lock 15 min; a successful login resets
the counter") but leaves two edge cases undefined, and AC4 only exercises the happy path
(5 fails → 6th correct within window refused → after window succeeds):

1. What happens to the lock when a login is attempted **while the account is already locked**
   (e.g. the AC4 6th attempt, or an attacker hammering during the window)?
2. What happens to the failure counter when the **15-minute window expires without a successful
   login** (the lock lifts by time, but is the counter cleared)?

## Decision

1. **Attempts during an active lock do NOT extend the lock and do NOT increment the counter.** The
   15 minutes are measured from the 5th failure, not from the last attempt. Implemented as a guard
   in `UserAccount.registerFailedLogin(now)`: if `isLocked(now)`, it is a no-op. This is what makes
   AC4 hold literally — "after 15 min it succeeds" is 15 min from the 5th failure regardless of the
   refused 6th attempt in between.
2. **Only a successful login clears the counter (literal BR8).** Lock expiry lifts the lock
   (`isLocked` becomes false once `now >= locked_until`) but leaves `failed_attempts` at 5, so the
   very next *failed* attempt after expiry reaches the threshold again and re-locks immediately.
   A *successful* login after expiry resets `failed_attempts` to 0 and clears `locked_until`.

Neutrality (BR7) is preserved throughout: the counter/lock bookkeeping runs only for an existing
account (`findByEmail` present); a non-existent e-mail touches no row. The lock state is a
deliberately distinct, observable outcome (`/login?locked`, `auth.account-locked` 423) that applies
only to real accounts that actually reached 5 failures — it is not an enumeration oracle because a
wrong password on a real *unlocked* account and any attempt on a non-existent account remain
byte-identical (`/login?error`), and the dominant timing cost (BCrypt) is paid in both via Spring
Security's user-not-found timing mitigation.

## Justification

Reading (1) is the only interpretation under which AC4's "after 15 min it succeeds" is true when a
refused attempt happens inside the window; extending on every attempt would let noise (or an
attacker) push a legitimate user's unlock indefinitely. Reading (2) is the literal text ("a
successful login resets") and is the safer default: a still-failing credential stays protected
rather than getting a fresh five-try budget the instant the window lapses. Both keep the domain
logic small and fully unit/mutation-testable in `UserAccount`.

## Alternatives discarded

- **Reset the counter when the window expires** — rejected: not stated by BR8, and it hands an
  attacker a fresh budget every 15 minutes; if the owner prefers this "clean slate on expiry"
  behavior it is a one-line change in `registerFailedLogin` behind this DL.
- **Re-lock/extend on attempts during the lock** — rejected: breaks AC4's "after 15 min it
  succeeds" and enables an indefinite-lock denial of service against a legitimate user.

## Impact

- Specs: none changed (edge cases were undefined; happy path unchanged).
- Files: `UserAccount` (`registerFailedLogin`, `registerSuccessfulLogin`, `isLocked`),
  `IdentityUserDetailsService` (`accountLocked` from `isLocked(now)`), the login failure/success
  event wiring, `AuditEventTypes.ACCOUNT_LOCKED`.
- Migrations: none (the `failed_attempts` / `locked_until` columns already exist since V3).

## How to revert

Flip reading (2) by clearing `failed_attempts` when a post-expiry attempt arrives (reset on the
first non-locked failure whose previous `locked_until` has passed) — localized to
`UserAccount.registerFailedLogin`. Reading (1) is load-bearing for AC4 and should not be reverted
without re-checking that acceptance criterion.
