# ADR 0004: Dev e-mail delivery — Mailpit + Spring Mail behind a `MailSender` port

## Status

Accepted

## Context

SPEC-0002 first access creates an account in `EMAIL_NOT_VERIFIED` and must deliver a
verification link (24 h) before the account can authenticate (BR5/BR6). The slice therefore
needs a way to (a) send mail from the identity flow without coupling the domain to SMTP, and
(b) actually observe the sent message in dev and in the isolated E2E stack so the first-access
journey can be exercised end to end (fetch the link, confirm, log in). No mail capability
exists yet, and the centralized notifications hub (in-app + e-mail templating, outbox) is owned
by SPEC-0004 (Phase 2) — building it now would be speculative (Rule Zero, simulation-and-mocking.md).
Baseline §0007 already fixes the shape: one `EmailSender` port, one SMTP implementation,
environment-config only, best-effort so the system runs without SMTP configured.

## Decision

We will add `spring-boot-starter-mail` and a single **`MailSender` port** (`infra.email`) with a
**`SmtpMailSender`** adapter over Spring's `JavaMailSender`, active only when `spring.mail.host`
is set; when it is absent a **`LoggingMailSender`** fallback logs a "mail not configured" notice
so every environment boots (baseline §0007). `domain.identity` publishes **`AccountCreated`**
(`@TransactionalEventListener` **AFTER_COMMIT**, baseline §0009); an **identity-scoped listener**
in `infra.email` builds the verification link (`app.identity.verification-base-url` +
`/verificar-email?token=…`, 24 h) and sends it via the port — the payload carries only the
account id, delivery e-mail and the raw one-time token, **never** the password or full CPF. In
dev and E2E we run **Mailpit** (SMTP `1025`, HTTP UI/API `8025`) as the catcher: `docker-compose.yml`
and `compose.e2e.yaml` gain a `mailpit` service, the app points `spring.mail.host` at it, and the
Playwright journey reads the link from Mailpit's HTTP API. Production points `spring.mail.host`
at a real SMTP host injected by env, whose presence `ProdReadinessValidator` will require. This
listener is an **explicit, traceable seam**: SPEC-0004 will graduate it into the notifications
module (in-app + templates + outbox) without changing the `AccountCreated` contract.

## Consequences

- Positive: the domain never imports SMTP; the first-access journey is observable in dev/E2E via
  Mailpit; boot never depends on a mail server; the seam is a clean hand-off point for SPEC-0004.
- Positive: sending is best-effort and off the request's transaction (AFTER_COMMIT), so a mail
  outage never rolls back or fails account creation.
- Negative: no retry/outbox yet — a dropped SMTP send is only logged (acceptable for the POC;
  SPEC-0004 adds the outbox + dead-letter of baseline §0007).
- Negative: two new dev/E2E containers (Mailpit) and one more env knob (`spring.mail.host`,
  `app.identity.verification-base-url`) to configure per environment.
- The raw verification token travels in an in-process event payload (never persisted as an
  outbox row this slice); only its SHA-256 hash is stored.

## Alternatives Considered

- **Build the SPEC-0004 notifications module now** — rejected: speculative scope for a consumer
  that does not exist yet; violates Rule Zero and the deferral rule.
- **GreenMail / an embedded SMTP in tests only** — rejected for the E2E catcher: Mailpit gives a
  real HTTP API the browser-level journey can query and a UI for manual dev inspection; unit/IT
  tests still use an in-memory recording `MailSender` double (no container needed).
- **Log-only "email" with no SMTP at all** — rejected: the AC1 journey (open the link, confirm,
  log in) cannot be exercised end to end without a retrievable message.
- **A generic `NotificationProvider` port now** — rejected: premature abstraction; a plain
  `MailSender` is the smallest correct seam, and SPEC-0004 owns the generalization.

## Revision Triggers

- SPEC-0004 (Notifications) lands — this listener graduates into the notifications module.
- A delivery-guarantee requirement appears (bounce handling, retries) — add the outbox +
  dead-letter of baseline §0007.
- A second app instance is deployed — revisit best-effort in-process dispatch.

## References

SPEC-0002 (BR5/BR6/§Events) · SPEC-0004 (owns the centralized hub) · SPEC-0006 (legal-document
pages) · DECISIONS-BASELINE §0007 (e-mail in layers) / §0009 (AFTER_COMMIT events) · ADR-0001
(module map) · `docs/architecture/messaging-and-integrations.md` §Notifications.
