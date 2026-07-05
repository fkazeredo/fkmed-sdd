# ADR 0008: Module map revision — `domain.notification` (shared in-app + async e-mail)

## Status

Proposed

## Context

SPEC-0004 introduces a product-wide notification mechanism: domain events from any module become
**in-app notifications** (bell + center) and **e-mails**, with per-event-type channel preferences
and a mandatory-type lock. This is a new business capability with its own data ownership (the
`notification`, `notification_event_type` registry, and `notification_preference` tables) and its
own rules — mandatory security/account types cannot be opted out (BR7); e-mail is asynchronous
AFTER_COMMIT and a delivery failure must not roll back the business transaction that raised the
event (BR6); notification content carries no full CPF/CNS/bank data (BR4). It does not belong to
`domain.identity`, `domain.plan`, `domain.audit` or `domain.content`. ADR-0001's "grow the module
map one spec at a time" policy is triggered.

## Decision

We will add **`domain.notification`** as a verified Modulith module (`explicitly-annotated`,
consistent with the map) owning its three tables and the seeded event-type catalog — **registry
data** (code, description, `email_default`, `mandatory`) per DECISIONS-BASELINE §0019, never an
enum. It consumes other modules' domain events via Spring's `@TransactionalEventListener(AFTER_COMMIT)`
with `@Transactional(REQUIRES_NEW)` — the Modulith `@ApplicationModuleListener` is avoided because its
`spring-modulith-events` starter is not on the classpath and pulling it in for the same after-commit +
own-transaction semantics would be over-engineering (Rule Zero); delivery is in-process. Each consumed
event creates an in-app notification and — when the type's
`email_default` is set and the user has not opted out — dispatches an e-mail through the existing
`infra.email.MailSender` seam (ADR-0004). E-mail dispatch failures are caught and logged, never
propagated, so they cannot roll back the publishing transaction (BR6). Its public API is
`NotificationController` (`/api/notifications`, `/read`, `/read-all`, `/preferences`). The verified
map becomes **seven modules**: plan, error, identity, audit, content, card, notification
(`domain.card` is added the same phase by ADR-0010).

## Consequences

- **Positive:** one mechanism for every future notification trigger; the registry catalog lets new
  event types land by migration without code churn; AFTER_COMMIT + swallowed failures keep business
  transactions safe; in-process events avoid broker infrastructure at POC scale.
- **Negative:** a sixth module raises the `ModularityTest`/diagram surface; per DL-0008 the existing
  identity `*EmailListener`s stay in place for transactional account e-mails, so two e-mail paths
  coexist until a future consolidation.

## Alternatives Considered

- **Fold notifications into `domain.identity` or `domain.content`** — rejected: a different bounded
  context and lifecycle; would make those modules dumping grounds.
- **Synchronous, in-transaction e-mail** — rejected: a mail failure would roll back business state,
  violating BR6.
- **A message broker (RabbitMQ/Kafka) for delivery** — rejected (Rule Zero): in-process Spring
  events suffice at POC scale; the module boundary lets the transport be swapped later without
  touching event producers.

## Revision Triggers

- Notification volume/latency needs out-of-process delivery → introduce a broker behind the module.
- Push/SMS/WhatsApp channels arrive (out of scope today).
- The two coexisting e-mail paths (DL-0008) are consolidated into this module.

## References

SPEC-0004 · ADR-0001 (module map, revised here) · ADR-0004 (Mailpit e-mail seam) ·
DECISIONS-BASELINE §0019 (registry vs enum) · DL-0006/DL-0007/DL-0008 ·
`docs/architecture/modules-and-apis.md` · diagram `docs/architecture-diagrams/modules.puml`.
