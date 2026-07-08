# ADR 0014: Module map revision — `domain.telemedicine` (Pronto Atendimento queue + tele sessions)

## Status

Accepted

## Context

SPEC-0010 introduces telemedicine: a 24/7 Pronto Atendimento **virtual queue** (triage → term →
queue → room → closure) and **scheduled teleconsultation**, with the beneficiary always seeing the
true session state pushed in near real time. It is the first module with a real-time **push (SSE)**
surface, a session **state machine**, and a **business-event producer** feeding notifications and
document issuance. New bounded context; ADR-0001's growth policy triggered.

## Decision

Add **`domain.telemedicine`** as the 11th verified Modulith module owning `tele_session`
(state machine `EM_FILA → EM_ATENDIMENTO | ABANDONADA`; `EM_ATENDIMENTO → ENCERRADA`; finals
`ENCERRADA`/`ABANDONADA` — an allowed enum under invariant 7, keep-criterion in Javadoc), the
`symptom` and `tele_term` registries, and the queue/room lifecycle. Key decisions:

- **Room is state-driven, no media** (ADR-0015): professional+CRM, timer, states — no WebRTC.
- **Queue transport is SSE push** (ADR-0016, DL-0022): position/ETA/state streamed, re-emitted
  periodically; 2-minute reconnection hold (DL-0017).
- **Single active session** per beneficiary; a second Pronto Atendimento **resumes** it (BR7),
  guarded by an optimistic lock (the start-next race — DL-0005 precedent).
- **Closure issues clinical documents atomically** into `domain.clinicaldocs` (BR10) — a
  one-directional `telemedicine → clinicaldocs` dependency.
- **Scheduled teleconsultation reuses `domain.appointment`** (DL-0018: virtual Telemedicina unit +
  modality flag + `join` window) — no second scheduling engine.
- **Events** `TeleTurnReached`/`TeleSessionClosed` AFTER_COMMIT → a listener in `domain.notification`
  turns them into in-app + e-mail (new catalog types), following the existing listener pattern.
- **Professional-side transitions** (start attending, close) are driven by the SPEC-0018 tele slice
  (ADR-0017, DL-0021), not by a beneficiary path.

Module dependencies: `telemedicine → appointment` (scheduled), `→ clinicaldocs` (closure issuance),
`→ plan` (beneficiary scope), `→ audit`; events consumed by `notification`. No cycles.

## Consequences

- **Positive:** queue, room, closure and scheduled tele land in one cohesive module reusing proven
  patterns (state-machine enum, optimistic lock, notification wiring, appointment model, scope).
- **Negative:** the SSE push surface is genuinely new infrastructure (emitters, cleanup, reconnection);
  the widest fan-out so far (appointment/clinicaldocs/plan/audit + notification events); an 11th module.

## Alternatives Considered

- Real audio/video room — rejected (ADR-0015: WebRTC is post-POC).
- Polling for the queue — rejected (owner chose SSE push).
- A dedicated tele scheduling model — rejected (DL-0018: reuse `domain.appointment`).

## Revision Triggers

- Real media (WebRTC) or a message broker for push would revise ADR-0015/0016.
- Group/async care modalities would extend the session model.

## References

SPEC-0010 · SPEC-0009 (appointment reuse) · SPEC-0011 (issuance) · ADR-0001 (module map) ·
ADR-0015 (room) · ADR-0016 (SSE) · ADR-0017 (operator-sim) · DL-0017/0018/0022 · DL-0005 (optimistic
lock) · diagram `docs/architecture-diagrams/modules.puml`.
