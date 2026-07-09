# 0009 - Appointments

**Status:** Approved

## Goal

Beneficiaries book consultations and exams in the operator's own care network — choosing
specialty/exam, unit, date and time against real slot capacity — and track, cancel or
reschedule their commitments.

## Scope

- Appointments hub (Agendar Consulta, Agendar Exame, Meus Agendamentos, Telemedicina link).
- Consultation wizard and exam wizard (with mandatory medical-order upload).
- Slot capacity control safe under concurrency.
- Meus Agendamentos (upcoming/history), cancellation and rescheduling.
- Own units + 30-day agenda seed.

## Business Context

Scheduling covers the operator's **own units** only; accredited external providers are
contacted directly (SPEC-0008 guides the user there). Slots have finite capacity; two
users may race for the last seat. Every booking is made **for** the active beneficiary
(possibly by the titular) and generates a protocol.

## Business Rules

- **BR1** — Every appointment MUST be bound to the active beneficiary at confirmation time
  and record its author (SPEC-0003 BR4).
- **BR2** — The wizard MUST NOT advance without the current step's mandatory selection,
  showing a clear message.
- **BR3** — Consultation wizard steps: specialty (registry shared with SPEC-0008) → unit
  (own units serving that specialty) → date/time → review → confirm.
- **BR4** — Exam wizard steps: exam (registry catalog: Hemograma, Raio-X, Ultrassonografia,
  Ressonância Magnética, Tomografia, …) → **medical order attachment (mandatory)** → unit →
  date/time → review → confirm. The attachment accepts JPG/PNG/PDF up to 5 MB, validated by
  real file content, with name preview and removal.
- **BR5** — The calendar offers **today through +30 days**; past dates and days without
  agenda are not selectable. Slot times come from each unit's agenda; full slots MUST
  render unavailable.
- **BR6** — Confirmations MUST NOT exceed a slot's capacity: under concurrent confirmation
  of the last seat, exactly one succeeds; the other receives "horário acabou de ser
  preenchido" and returns to the time step.
- **BR7** — Confirming MUST create the appointment as `AGENDADO`, generate the unique
  protocol `AG-AAAAMMDD-####` (SPEC-0003 BR9) and occupy the seat.
- **BR8** — A beneficiary MUST NOT hold two active appointments (`AGENDADO`/`REAGENDADO`)
  at the same date and time.
- **BR9** — Cancellation is allowed until the start time (optional reason ≤ 200 chars);
  it MUST release the seat, set `CANCELADO` and keep the record in history. After the
  start time the action is unavailable.
- **BR10** — Rescheduling MUST keep beneficiary, specialty/exam, unit and **protocol**;
  only the date/time step reopens; confirming releases the previous seat, occupies the new
  one and sets `REAGENDADO`.
- **BR11** — States: `AGENDADO → REAGENDADO | CANCELADO | REALIZADO`;
  `REAGENDADO → CANCELADO | REALIZADO`. Finals: `CANCELADO`, `REALIZADO`. (Status is a
  state machine — enum with the keep criterion documented in Javadoc.)
- **BR12** — Once the start time passes without cancellation, the appointment MUST appear
  in history as `REALIZADO`.
- **BR13** — Meus Agendamentos MUST list commitments of **all** beneficiaries accessible
  to the user, with a beneficiary filter; tab **Próximos** ordered soonest-first, tab
  **Histórico** most-recent-first. Cards show type, specialty/exam, beneficiary, unit,
  date/time, the Telemedicina badge when applicable (SPEC-0010) and status.

## Input/Output Examples

- `POST /api/appointments` `{beneficiaryId, type:"CONSULTATION", specialty:"CARDIOLOGIA",
  unitId, slot:"2026-07-10T09:00"}` → `201 {"protocol":"AG-20260704-0001","status":"AGENDADO"}`.
- Same slot, last seat already taken → `409 {"code":"appointment.slot-taken"}` (error case).
- Exam confirmation without attachment → `422 {"code":"appointment.attachment-required"}`
  (error case).
- `POST /api/appointments/{id}/cancel` after start time → `409
  {"code":"appointment.cancel-too-late"}` (error case).

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/appointments/units` | Units serving a specialty/exam |
| GET | `/api/appointments/availability` | Days + time slots with remaining capacity |
| POST | `/api/appointments` | Confirm booking (multipart when exam attachment) |
| GET | `/api/appointments` | Upcoming/history, beneficiary filter |
| POST | `/api/appointments/{id}/cancel` | Cancel with optional reason |
| POST | `/api/appointments/{id}/reschedule` | New slot, same protocol |

## Events

`AppointmentConfirmed`, `AppointmentCancelled`, `AppointmentRescheduled` (AFTER_COMMIT) →
SPEC-0004 (e-mail + in-app: protocol, beneficiary, specialty/exam, unit, date/time).
Reminder 24 h before *(MAY, future)*.

## Persistence Changes

Migration (number at implementation): `care_unit` (2 seeded own units with address);
`unit_agenda`/`schedule_slot` (unit, specialty/exam scope, date, time, capacity, occupied —
consistency guarded by optimistic/atomic update); `exam_type` registry; `appointment`
(id, protocol unique, type `CONSULTATION|EXAM`, beneficiary_id, specialty_code/exam_code,
  unit_id, scheduled_at, status, medical-order storage reference (SPEC-0019), cancel_reason, created_by,
timestamps). Seed: agenda for the next 30 days, Mon–Sat, 08:00–17:00, 30-min slots.

## Validation Rules

Slot within unit agenda and horizon (BR5). Attachment: JPG/PNG/PDF ≤ 5 MB content-checked.
Reason ≤ 200. Type-specific mandatory fields (specialty×exam). `beneficiaryId` scope-checked.

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Slot full at confirmation | `appointment.slot-taken` | 409 |
| Same beneficiary, same date/time active | `appointment.time-conflict` | 409 |
| Exam without medical order | `appointment.attachment-required` | 422 |
| Attachment wrong type/size | `appointment.attachment-invalid` | 422 |
| Date outside horizon/agenda | `appointment.outside-horizon` | 422 |
| Cancel after start | `appointment.cancel-too-late` | 409 |

## Observability Requirements

Business log per transition (protocol, masked beneficiary); counters: bookings, cancels,
reschedules, slot-race conflicts (BR6 signal).

## Tests Required

- **Domain/unit:** state machine transitions; horizon rules; conflict rule.
- **Integration (Testcontainers):** capacity race (concurrent last-seat confirmation);
  cancel releases seat; reschedule keeps protocol; REALIZADO derivation.
- **API contract:** all endpoints and error codes.
- **Frontend unit:** wizard step gating; attachment preview/remove; tabs ordering/filter.
- **E2E:** full consultation booking; exam blocked without attachment; cancel + rebook.

## Acceptance Criteria

- **AC1** (BR7) — Given the complete consultation wizard, when I confirm, then I get a
  protocol and the item shows in Próximos as `AGENDADO`, with that slot's capacity reduced.
- **AC2** (BR4) — Given an exam without the medical order attached, then advancing is
  blocked with a message (error case).
- **AC3** (BR6) — Given the last seat confirmed by two users simultaneously, then exactly
  one succeeds and the other sees the slot-taken warning (error case).
- **AC4** (BR9) — Given an upcoming appointment, when I cancel and confirm, then it moves
  to Histórico as `CANCELADO` and the slot becomes available again.
- **AC5** (BR8) — Given I already have a consultation on 10/07 at 09:00, when I try another
  commitment at the same date/time, then the system blocks it (error case).
- **AC6** (BR10) — Given a confirmed reschedule, then the protocol is unchanged, status is
  `REAGENDADO` and the old slot is free.
- **AC7** (BR1) — Given MARIA booking with PEDRO active, then the appointment belongs to
  PEDRO and records MARIA as author.
- **AC8** (BR12) — Given an appointment whose start time passed without cancellation, then
  it appears in Histórico as `REALIZADO`.

## Open Questions

- **OQ1** *(resolved — DL-0013, owner-decided)* — Minimum booking antecedence = **2 hours**:
  availability filtering blocks slots less than 2 h ahead.

## Out of Scope

Booking at external accredited providers; waiting list for full slots; presence
confirmation/check-in; teleconsultation booking mechanics beyond delegation (owned by
SPEC-0010).
