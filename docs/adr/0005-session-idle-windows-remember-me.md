# ADR 0005: Two session idle windows via Spring Session remember-me

## Status

Proposed

## Context

SPEC-0002 BR12 requires two mutually exclusive session lifetimes chosen by the user at login
through a "Manter conectado" checkbox on the embedded Authorization Server's form-login page:

- **unchecked** — the session ends when the browser closes AND expires after **30 minutes** of
  inactivity (owner decision, folded into BR12);
- **checked** — the session survives browser restarts for up to **7 days** of inactivity.

Sessions are server-side, JDBC-backed (Spring Session JDBC, `spring_session` /
`spring_session_attributes`, DECISIONS-BASELINE §0020) so the app stays multi-instance ready.
A single fixed `server.servlet.session.timeout` cannot express two windows, and a session cookie
without `Max-Age` always dies on browser close — so "remain connected for 7 days across restarts"
needs a **persistent** cookie only in the checked case. The mechanism is a structural choice
(where the two windows live, how the cookie is written) that the baseline wants recorded, because
getting it wrong silently degrades either security (session lives too long) or UX (user logged out
too soon).

## Decision

We will express the two windows with **Spring Session's remember-me integration**, not a custom
filter. The default window is the framework timeout: `server.servlet.session.timeout: 30m`
(explicit in `application.yaml`) governs every session and, being a session cookie, dies on browser
close — the exact BR12 "unchecked" behavior. The "checked" window is a
`SpringSessionRememberMeServices` bean (`validitySeconds = 604800`, 7 days) wired into the
form-login chain via `http.rememberMe(...)`; when the login form posts the `remember-me` parameter
it (a) raises that session's `maxInactiveInterval` to 7 days and (b) marks the request so the
`DefaultCookieSerializer` (a bean, told the remember-me request-attribute name) writes a
**persistent** `SESSION` cookie with a 7-day `Max-Age` that survives browser restarts. The checkbox
lives on the Thymeleaf login page as `name="remember-me"`. Session expiry mid-use surfaces to the
SPA as a `401` on the stateless `/api/**` chain (and a `/login` redirect on the AS authorize
endpoint) — the SPA owns the "Sua sessão expirou" UX and return-route restore.

## Consequences

- **Positive:** zero custom session machinery — the two windows are one config value plus two
  framework beans; the persistent-cookie concern is handled by Spring Session exactly as
  documented; both windows are asserted end-to-end against real Postgres (the persisted
  `spring_session.max_inactive_interval` is `1800` vs `604800`, and the `Set-Cookie` carries
  `Max-Age` only when checked).
- **Negative / risk:** the behavior depends on the login form actually submitting the
  `remember-me` parameter name and on the `DefaultCookieSerializer` bean being the one Spring
  Session uses — both are covered by the integration test, so a regression fails the build. The
  7-day persistent cookie is a longer-lived credential on shared devices; it is opt-in and the
  cookie stays `HttpOnly` + `Secure` (prod) + `SameSite=Lax`. Remember-me here does NOT mean
  Spring Security's token-based auto-login (no `remember-me` DB token table); it only extends the
  server session — a deliberately smaller surface.

## Alternatives Considered

- **A custom authentication-success handler calling `session.setMaxInactiveInterval(...)` plus a
  hand-rolled persistent cookie** — rejected: it re-implements precisely what
  `SpringSessionRememberMeServices` + `DefaultCookieSerializer` already do (including writing the
  cookie `Max-Age`), adding untested cookie code for no gain (Rule Zero).
- **Spring Security's token-based remember-me (`rememberMe().tokenRepository(...)`)** — rejected:
  it introduces a separate persistent-login token and auto-login path parallel to the session,
  which is more surface than BR12 needs; we want the *session itself* to live longer, not a second
  credential.
- **Two distinct fixed timeouts selected by a per-request attribute in a filter** — rejected: does
  not solve the persistent-cookie half of BR12 (a session cookie still dies on browser close), so
  "checked survives restart" would silently fail.

## Revision Triggers

- BR12 gains a third lifetime or makes the window server-configurable per environment.
- A real token-based auto-login (credential re-presentation without a live session) becomes a
  requirement.
- The session store moves off Spring Session JDBC.

## References

- `backend/src/main/java/com/fkmed/infra/security/SessionConfig.java`,
  `SecurityConfig.formLoginChain`, `backend/src/main/resources/templates/login.html`.
- `backend/src/main/resources/application.yaml` (`server.servlet.session.timeout`).
- SPEC-0002 BR12; DECISIONS-BASELINE §0020 (Spring Session JDBC, multi-instance).
- Spring Session reference — "Remember-me authentication" (`SpringSessionRememberMeServices`,
  `DefaultCookieSerializer.setRememberMeRequestAttribute`).
