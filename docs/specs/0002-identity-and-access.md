# 0002 - Identity and Access

**Status:** Approved

## Goal

Beneficiaries pre-loaded by the operator create their own access account (first access),
authenticate securely, recover and manage their password, and have their sessions governed
predictably — with a complete audit trail of every identity event.

## Scope

- First access: identity verification (CPF + card number + birth date), account creation
  (login e-mail + password), acceptance of current Terms of Use and Privacy Policy,
  e-mail verification link (24 h).
- Login with "keep me connected" option; lockout after repeated failures.
- Password recovery (30-min single-use link) and authenticated password change.
- Session lifecycle (expiry, return route) and logout.
- "Segurança" screen (change password, read-only login e-mail, mobile-biometrics info card).

## Business Context

Accounts belong to beneficiaries seeded by the operator (SPEC-0001). One account per
beneficiary: a titular never logs in "as" a dependent — they access dependents' data
through their own account (SPEC-0003). Minors have no account and are served through the
titular's login.

## Business Rules

- **BR1** — First access MUST validate the exact triple CPF + card number + birth date
  against the beneficiary base; any mismatch MUST be refused with a single generic
  "registration data not found" error that does not indicate which field diverged.
- **BR2** — If the matched beneficiary already has an account, the system MUST NOT create
  another and MUST direct the user to login or password recovery.
- **BR3** — A dependent younger than 18 on the registration date MUST be refused with
  guidance that minors are served through the titular's account.
- **BR4** — The login e-mail MUST be unique system-wide; reusing an existing one MUST be
  refused.
- **BR5** — A new account is created in state `EMAIL_NOT_VERIFIED`; the verification link
  is valid for **24 hours**; opening a valid link activates the account; resending issues a
  new link and invalidates the previous one.
- **BR6** — Login of an unverified account MUST be refused with a specific message and a
  "resend verification e-mail" action.
- **BR7** — Failed login MUST always answer the single neutral message "invalid access
  data" — never revealing whether the e-mail exists or which credential failed. Password
  recovery MUST always answer neutrally ("if the e-mail is registered, we will send
  instructions").
- **BR8** — After **5 consecutive** login failures for the same e-mail, the account MUST be
  locked for **15 minutes**; attempts during the lock (even with the correct password) MUST
  be refused with the lock message. A successful login resets the failure counter.
- **BR9** — Password policy: minimum 8 characters, at least 1 letter and 1 digit; MUST NOT
  equal the login e-mail nor appear in the common-passwords list; a new password MUST
  differ from the current one.
- **BR10** — The password-reset link MUST expire in **30 minutes** and be invalidated after
  first use; a successful reset MUST terminate **all** active sessions of the user.
- **BR11** — Authenticated password change MUST require the correct current password.
- **BR12** — With "keep me connected" checked, the session survives browser restarts for up
  to **7 days** of inactivity; unchecked, the session ends when the browser closes and, even
  within the same browsing session, expires after **30 minutes** of inactivity. A session
  expiring mid-use MUST redirect to login with the notice "Sua sessão expirou", preserving
  the return route.
- **BR13** — Internal routes MUST require authentication; a visitor hitting one is sent to
  login and, after authenticating, returns to the original route.
- **BR14** — All identity events MUST be audited (SPEC-0003 trail): account created, e-mail
  verified, login success/failure, lockout, recovery requested, password reset/changed,
  logout — with timestamp, IP and device/browser identification.
- **BR15** — Registration MUST record acceptance of the **current versions** of the Terms
  of Use and Privacy Policy (version + timestamp); the re-acceptance flow for new versions
  is owned by SPEC-0006.
- **BR16** — Password fields MUST offer show/hide; forms MUST validate format client-side
  before submit (valid e-mail, 11-digit CPF, BR9 policy) with messages next to the field.

## Input/Output Examples

- `POST /api/auth/first-access/verify` `{"cpf":"…","cardNumber":"001234575","birthDate":"2007-05-20"}`
  → `200 {"registrationToken":"…"}`; wrong birth date → `422 {"code":"auth.registration-not-found"}` (error case).
- `POST /api/auth/first-access/complete` `{registrationToken, email, password, acceptTermsVersion, acceptPrivacyVersion}`
  → `201` account `EMAIL_NOT_VERIFIED` + verification e-mail queued.
- Login with wrong password → `401 {"code":"auth.invalid-credentials"}`; 6th attempt within
  lock with correct password → `423 {"code":"auth.account-locked"}` (error case).
- Reused reset link → `410 {"code":"auth.reset-link-invalid"}` (error case).

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/first-access/verify` | Validate identity triple → registration token |
| POST | `/api/auth/first-access/complete` | Create account (e-mail, password, acceptances) |
| POST | `/api/auth/verification/confirm` | Activate account from e-mailed token |
| POST | `/api/auth/verification/resend` | New verification link (invalidates previous) |
| POST | `/api/auth/recovery/request` | Neutral-response recovery request |
| POST | `/api/auth/recovery/reset` | Reset password from e-mailed token |
| PUT | `/api/auth/password` | Authenticated change (current + new) |
| POST | `/api/auth/logout` | End current session |

Login itself flows through the embedded Authorization Server (Code + PKCE); shapes are
finalized at implementation — the OpenAPI snapshot governs.

## Events

Published AFTER_COMMIT, consumed by SPEC-0004 for e-mail/in-app delivery:
`AccountCreated` (verification link), `PasswordRecoveryRequested` (reset link),
`PasswordChanged`, `AccountLocked`. Payloads carry account id and delivery e-mail — never
the password or full CPF.

## Persistence Changes

Migration (number assigned at implementation): `user_account` (id, beneficiary_id unique,
email unique, password_hash, status `EMAIL_NOT_VERIFIED|ACTIVE`, failed_attempts,
locked_until, created_at), `email_verification_token` and `password_reset_token` (hashed
token, expires_at, used_at), `term_acceptance` (account_id, document type, version,
accepted_at — shared with SPEC-0006). Common-passwords denylist as a seeded resource.
Sessions via Spring Session JDBC (baseline).

**Seed (owner decision, 2026-07-04):** the titular **MARIA** is seeded with a **real ACTIVE
account** (email verified) whose password is a **dev-only default** — enumerated in
`SECURITY.md`, allowlisted in `.gitleaks.toml`, and **refused under the prod profile** by the
fail-fast `ProdReadinessValidator`. **PEDRO is seeded with NO account** so the first-access
journey is exercised end-to-end. This retires the in-memory dev-login seam of SPEC-0001
(`DevLoginConfig`/`NoAccountsConfig`): the embedded Authorization Server now authenticates
against `user_account`.

## Validation Rules

CPF: 11 digits, valid check digits. Card: 9 digits. Birth date: valid, past. E-mail: valid
format, unique, ≤ 160 chars. Password: BR9. All enforced server-side; mirrored client-side
per BR16.

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Identity triple mismatch | `auth.registration-not-found` | 422 |
| Beneficiary already has account | `auth.account-already-exists` | 409 |
| Dependent under 18 | `auth.dependent-underage` | 422 |
| E-mail already in use | `auth.email-already-used` | 409 |
| Invalid credentials | `auth.invalid-credentials` | 401 |
| E-mail not verified | `auth.email-not-verified` | 403 |
| Account locked | `auth.account-locked` | 423 |
| Verification link expired/invalid | `auth.verification-link-invalid` | 410 |
| Reset link expired/used/invalid | `auth.reset-link-invalid` | 410 |
| Password policy violation | `auth.password-policy-violation` | 422 |
| Current password incorrect | `auth.current-password-incorrect` | 422 |

## Observability Requirements

Audit events of BR14 (personal data masked; CPF never logged in full). Metrics: login
success/failure counters, lockout counter, verification/recovery e-mails requested.

## Tests Required

- **Domain/unit:** password policy, lock counter/window, token expiry and single-use.
- **Integration (Testcontainers):** first-access happy path and each refusal; lock timing;
  reset terminates all sessions; resend invalidates previous link.
- **API contract:** snapshot covers all endpoints and error codes.
- **Frontend unit:** form validations, show/hide, neutral messages, return-route handling.
- **E2E:** PEDRO's full first access + verification + login; lockout journey; recovery with
  link reuse rejected; keep-connected vs session-only behavior.

## Acceptance Criteria

- **AC1** (BR1, BR5) — Given PEDRO (no account) enters his correct triple, when he completes
  registration and opens the e-mail link within 24 h, then he can authenticate.
- **AC2** (BR1) — Given a correct card number but wrong birth date, when verifying, then the
  generic "data not found" refusal appears (error case).
- **AC3** (BR2) — Given a beneficiary who already has an account, when attempting first
  access, then they are directed to login/recovery and no account is created.
- **AC4** (BR8) — Given 5 wrong-password logins, when the 6th uses the correct password
  within 15 min, then access is refused with the lock message; after 15 min it succeeds.
- **AC5** (BR10) — Given a reset link already used, when opened again, then it is rejected
  as invalid (error case).
- **AC6** (BR10) — Given I reset my password in one browser, then sessions in other
  browsers are terminated on their next action.
- **AC7** (BR12) — Given "keep me connected" unchecked, when I close and reopen the
  browser, then I must authenticate again; checked, I remain authenticated.
- **AC8** (BR7) — Given recovery requested for a non-existent e-mail, then the response is
  identical to the existing-e-mail case.
- **AC9** (BR3) — Given a 16-year-old dependent attempting first access, then registration
  is refused with the titular guidance (error case).
- **AC10** (BR14) — Given any event of BR14 occurs, then the audit trail records it with
  timestamp, IP and device identification.

## Open Questions

- ~~**OQ1** — Session inactivity timeout when "keep me connected" is off~~ — **answered by
  the owner (2026-07-04): 30 minutes** of inactivity (folded into BR12).

## Out of Scope

Two-factor authentication (recorded as post-POC evolution), social login, biometrics (the
Segurança screen only shows the "available in the mobile app" info card), change of login
e-mail (orient to service channels), CAPTCHA/anti-bot hardening.
