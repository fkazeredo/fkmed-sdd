# DL-0005 — Concurrent account-update translation (optimistic lock, débito técnico A)

- **Phase/slice:** Phase 1 · Slice 1.3 (identity hardening — carried from 1.2)
- **Spec(s):** SPEC-0002 (BR8 under concurrency)
- **Related ADR:** —
- **Date:** 2026-07-04
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

BR8 defines the lockout counter but not its behavior under CONCURRENT failed logins. `UserAccount` had
no `@Version`, so two simultaneous `recordFailedLogin` transactions read the same `failed_attempts` and
lost-update each other — N simultaneous wrong passwords could leave the counter below the threshold and
never lock the account. Undefined too: how to surface a conflict that cannot be reconciled.

## Decision

1. Add `@Version` to `UserAccount` (Flyway V6, `version bigint not null default 0`).
2. `recordFailedLogin` runs each attempt in its OWN transaction (`TransactionTemplate`) and retries on
   `ObjectOptimisticLockingFailureException`, re-reading the account on a fresh version and re-applying
   the increment — up to `MAX_LOCK_RETRIES = 10` (generous: only the first 5 increments can collide;
   once locked, further attempts are conflict-free no-ops).
3. A conflict still unresolved after the retry budget (or on another account mutation — change/reset/
   activate) is translated to `ConcurrentAccountUpdateException` → HTTP **409** `auth.concurrent-update`.
   The raw framework `ObjectOptimisticLockingFailureException` never leaks to the client.

## Justification

Retry-on-fresh-read preserves every increment (the regression `ConcurrentFailedLoginIT` proves 8
concurrent wrong passwords still drive the counter to 5 and lock exactly once). Per-attempt transactions
keep each retry's read/write atomic. 409 (retryable) is the honest semantic for an optimistic-lock
conflict and keeps the neutral login UX intact (the counter path is server-side; the user still sees the
generic invalid-credentials/locked message). Keeping the increment logic in
`UserAccount.registerFailedLogin` (not a raw SQL UPDATE) preserves the lock/audit state machine in one
place.

## Alternatives discarded

- Pessimistic DB lock (`SELECT ... FOR UPDATE`) — rejected: serializes all failed logins for an account
  and holds a row lock across the audit write; heavier than an optimistic retry for a low-contention path.
- Atomic `UPDATE user_account SET failed_attempts = failed_attempts + 1` — rejected: bypasses the
  `UserAccount` state machine (lock threshold, audit-once), scattering BR8 logic into SQL.
- Translate without retrying — rejected: the conflicting attempt would not be re-applied, so the
  lost-update (under-count) persists and the regression stays red.

## Impact

- Specs: none changed (BR8 concurrency was undefined).
- Files: V6 migration, `UserAccount` (`@Version`), `IdentityService` (recordFailedLogin retry,
  `TransactionTemplate`/`PlatformTransactionManager`), `ConcurrentAccountUpdateException`,
  `HttpErrorMapping` (409), `messages.properties`, `ConcurrentFailedLoginIT`.
- Migrations: V6 (additive column, default 0 — safe on existing rows).

## How to revert

Drop `@Version` + V6 and the retry loop (revert `recordFailedLogin` to a single `@Transactional`). This
reinstates the lost-update race, so it should not be reverted without an alternative concurrency control.
The retry budget and 409 code are localized to `IdentityService`/`HttpErrorMapping`.
