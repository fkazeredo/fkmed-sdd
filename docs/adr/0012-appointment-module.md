# ADR 0012: Module map revision — `domain.appointment` (scheduling with capacity + state machine)

## Status

Proposed

## Context

SPEC-0009 introduces booking/cancel/reschedule of consultations and exams in the operator's own
units, against **real slot capacity under concurrency**, with a **state machine**, a unique
**protocol**, a **medical-order upload**, and **business events** that feed the notification
mechanism. This is a new transactional bounded context — the first with real write concurrency and
the first business-event producer for SPEC-0004. ADR-0001's growth policy is triggered.

## Decision

We will add **`domain.appointment`** as the 9th verified Modulith module owning `care_unit`,
`unit_agenda`/`schedule_slot`, the `exam_type` registry and `appointment` (unique protocol, status).
Key decisions:

- **Slot capacity under concurrency:** an `@Version` optimistic lock on the slot + a domain method
  that occupies/releases seats, translating a conflict to **`SlotUnavailableException`** →
  `409 appointment.slot-taken`. **Fail-fast, no retry loop** (BR6: exactly one of two racers wins,
  the other is told the slot filled) — unlike the lockout retry (DL-0005), where retrying made
  sense. The existing `GlobalExceptionHandler` optimistic-lock safety net still applies.
- **Status is a state-machine enum** (`AGENDADO → REAGENDADO|CANCELADO|REALIZADO`; finals
  `CANCELADO`/`REALIZADO`) — an allowed enum under invariant 7 (lifecycle state machine), with the
  keep-criterion documented in its Javadoc.
- **Protocol** `AG-…` from the shared generator placed in `domain.plan` (DL-0016).
- **Medical-order upload** duplicates the magic-byte check + adds the PDF signature (DL-0015).
- **Specialty registry reused** from `domain.network` (`appointment → network`).
- **Events** `AppointmentConfirmed`/`AppointmentCancelled`/`AppointmentRescheduled` published
  AFTER_COMMIT; a listener in `domain.notification` turns them into in-app + e-mail (catalog
  `appointment.confirmed` already seeded; `.cancelled`/`.rescheduled` added by this phase),
  following the `ContactDataChangedNotificationListener` pattern.

Module dependencies: `appointment → plan` (beneficiary scope + protocol), `→ network` (specialty),
`→ audit` (author/audit), and events consumed by `notification`. No cycles.

## Consequences

- **Positive:** concurrency, protocol and events land in one cohesive module reusing proven
  patterns (optimistic lock, notification wiring, magic-byte, scope facade).
- **Negative:** the widest fan-in so far (plan/network/audit + notification events); a 9th module
  raises the ModularityTest/diagram surface; the slot-race path needs a real concurrency IT, not
  just unit tests.

## Alternatives Considered

- **Retry loop for slot capacity** (like lockout) — rejected: BR6 wants the loser told immediately,
  not silently retried into the same loss.
- **`appointment.status` as registry data** — rejected: it is a lifecycle state machine (invariant 7
  explicitly allows the enum here).
- **Protocol generator inside `appointment`** — rejected (DL-0016: BR9 is a SPEC-0003 rule → lives in
  `domain.plan`, shared with future `RE-`/`PV-`).

## Revision Triggers

- Slot throughput needs pessimistic locking or a queue (today optimistic fail-fast suffices).
- Teleconsultation booking (SPEC-0010) or reminders extend the module.

## References

SPEC-0009 · SPEC-0003 BR9 (protocol) · ADR-0001 (module map, revised) · ADR-0008 (notification) ·
ADR-0011 (network — specialty registry) · DL-0005 (optimistic lock) · DL-0013/0015/0016 ·
DECISIONS-BASELINE §0019 · diagram `docs/architecture-diagrams/modules.puml`.
