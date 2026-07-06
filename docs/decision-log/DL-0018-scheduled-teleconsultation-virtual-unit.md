# DL-0018 — Scheduled teleconsultation as a SPEC-0009 appointment on a virtual Telemedicina unit

- **Phase/slice:** Phase 4 · Telemedicine (SPEC-0010 BR14)
- **Spec(s):** SPEC-0010 (BR14), SPEC-0009 (appointment model)
- **Related ADR:** ADR-0014 (domain.telemedicine)
- **Date:** 2026-07-06
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0010 BR14 says scheduled teleconsultation is "created as a SPEC-0009 appointment with the
Telemedicina badge (protocol, cancellation and rescheduling rules inherited)". How to model it
without forking the appointment module?

## Decision

Reuse `domain.appointment` unchanged in shape: seed a **virtual "Telemedicina" care unit** whose
`unit_agenda`/`schedule_slot` back the tele agenda (30-day horizon), and mark the appointment with a
**Telemedicina modality flag** (a small additive column, not a new type — booking/cancel/reschedule/
protocol all inherited). `POST /api/appointments/{id}/join` enables from 10 min before the slot
until its end (BR14); the join opens the tele room, bridging to `domain.telemedicine`.

## Justification

Maximum reuse (Rule Zero): the entire booking/capacity/protocol/cancel/reschedule machinery already
exists and is tested. A modality flag + a virtual unit is the minimal delta; no second scheduling
engine. The `join` window is the only genuinely new rule.

## Alternatives discarded

- A separate tele-scheduling model — rejected: duplicates SPEC-0009 wholesale.
- A new appointment `type` value — rejected: consultation/exam are the booking flows; tele is a
  modality of a consultation, better as an orthogonal flag than a third type.

## Impact / How to revert

One additive column on `appointment` + a seeded virtual unit + the join endpoint. Reversible by
dropping the flag/unit; the appointment rows remain valid consultations.
