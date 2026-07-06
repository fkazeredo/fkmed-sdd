# 0010 - Telemedicine

**Status:** Approved (Phase 4)

## Goal

Remote medical care in two modalities: immediate **Pronto Atendimento** through a 24/7
virtual queue (triage → term → queue → room → summary with issued documents) and
**scheduled teleconsultation** — with the beneficiary always seeing the true state of
their session.

## Scope

- Telemedicine hub with instability-notice banner.
- Triage (complaint, symptoms, duration, emergency-signal alert).
- Versioned teleattendance term acceptance.
- Virtual queue (position/ETA near-real-time, leave, no-show expiry) and room lifecycle.
- Closure summary + document handoff to SPEC-0011.
- Scheduled teleconsultation on top of SPEC-0009.

## Business Context

The professional side (conducting the call, closing the session, issuing documents) belongs
to the operator's team and has no interface in this POC — those transitions are driven by
the operator-simulation API (SPEC-0018). The beneficiary-facing states and rules below are
real system behavior.

## Business Rules

- **BR1** — The hub MUST offer Pronto Atendimento, Agendar Consulta (tele) and Meus
  Agendamentos (filtered by Telemedicina). When the "Instabilidade momentânea da
  Telemedicina" notice (SPEC-0005) is active, an alert banner MUST appear on the hub and
  the queue — informative, never blocking.
- **BR2** — Triage MUST collect: attended beneficiary (active, confirmable), main
  complaint (**10–500 chars**), symptoms (multi-select from registry catalog + optional
  "outros" text) and symptom duration (horas · 1–3 dias · +3 dias · +1 semana).
- **BR3** — Selecting emergency signals MUST raise an alert directing to a 24h ER
  (SPEC-0008), with the option to proceed at the user's own decision.
- **BR4** — Entering the queue REQUIRES accepting the current teleattendance term;
  acceptance is recorded (version + timestamp) and remains valid while that version is
  current; a new version demands a new acceptance at the next session.
- **BR5** — Queue entry creates the session as `EM_FILA` showing queue position, estimated
  wait and **Sair da fila** (with confirmation → `ABANDONADA`, position released).
- **BR6** — Position and ETA MUST update automatically (near real time), without user
  action.
- **BR7** — Only **one** active queue session per beneficiary; attempting another MUST
  resume the existing one with its position preserved.
- **BR8** — When it is the beneficiary's turn, the system MUST highlight it visually, emit
  a notification and open the room; **5 minutes** without response expires the session as
  `ABANDONADA` with a notice.
- **BR9** — The room MUST show the professional (name + CRM), start time and running
  duration, plus "Encerrar minha participação". Closure by the professional/team sets
  `ENCERRADA` and leads to the summary: professional, duration, guidance and **documents
  issued**, with the link "Ver em Minha Saúde".
- **BR10** — Documents issued at closure MUST be immediately available in SPEC-0011, bound
  to the attended beneficiary and to the session.
- **BR11** — Session states: `EM_FILA → EM_ATENDIMENTO | ABANDONADA`;
  `EM_ATENDIMENTO → ENCERRADA`. Finals: `ENCERRADA`, `ABANDONADA`. (State machine — enum.)
- **BR12** — Pronto Atendimento is available 24/7.
- **BR13** — A minor beneficiary only enters care in a session initiated by the titular
  (the titular conducts with the minor as active beneficiary — authorship audited).
- **BR14** — Scheduled teleconsultation: specialty available in telemedicine → date/time
  from the tele agenda (30-day horizon) → review → confirm. It is created as a SPEC-0009
  appointment with the **Telemedicina** badge (protocol, cancellation and rescheduling
  rules inherited); **"Entrar na consulta"** MUST be enabled from **10 minutes before**
  the slot until its end.

## Input/Output Examples

- `POST /api/tele/sessions` `{beneficiaryId, complaint:"Dor de cabeça há 2 dias…",
  symptoms:["CEFALEIA"], duration:"D1_3", termVersion:"1.0"}` →
  `201 {"state":"EM_FILA","position":4,"etaMinutes":12}`.
- Complaint with 5 chars → `422 {"code":"tele.complaint-invalid"}` (error case).
- Second `POST` while queued → `200` with the **existing** session (BR7).
- "Entrar na consulta" at 14:45 for a 15:00 slot → `409 {"code":"tele.join-window-closed"}`
  (error case).

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/tele/catalog` | Symptom registry + current term text/version |
| POST | `/api/tele/sessions` | Triage + term acceptance → enter queue (or resume) |
| GET | `/api/tele/sessions/current` | State, position, ETA, room data (poll/stream) |
| POST | `/api/tele/sessions/current/leave` | Leave queue (`ABANDONADA`) |
| POST | `/api/appointments/{id}/join` | Join scheduled teleconsultation room |

Professional-side transitions (start attending, close with documents) are SPEC-0018
endpoints.

## Events

`TeleTurnReached` (your turn), `TeleSessionClosed` (summary + document links) —
AFTER_COMMIT → SPEC-0004. Teleconsultation booking events inherited from SPEC-0009.

## Persistence Changes

Migration (number at implementation): `tele_session` (id, beneficiary_id, type
`WALK_IN|SCHEDULED`, state, complaint, symptom codes, duration_code, professional_name,
professional_crm, queue_entered_at, called_at, started_at, ended_at, term_version,
created_by); registry `symptom`; `tele_term` (version, published_at, body) + seed; tele
agenda reusing the SPEC-0009 slot model (virtual unit) — modeling refined at
implementation.

## Validation Rules

Complaint 10–500 chars. Symptoms from registry. Duration from fixed list. Term version must
equal current. `beneficiaryId` scope-checked (SPEC-0003).

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Complaint out of bounds | `tele.complaint-invalid` | 422 |
| Term not accepted / stale version | `tele.term-not-accepted` | 422 |
| Join outside allowed window | `tele.join-window-closed` | 409 |
| No active session to act on | `tele.session-not-found` | 404 |

## Observability Requirements

Metrics: queue size, average wait, abandonment (left × no-show), sessions closed. Business
log per state transition with session id (no clinical content in logs — complaint text is
sensitive).

## Tests Required

- **Domain/unit:** state machine; no-show expiry timing; single-active-session rule.
- **Integration (Testcontainers):** queue enter/resume/leave; turn + 5-min expiry;
  closure creates SPEC-0011 documents atomically.
- **API contract:** all endpoints.
- **Frontend unit:** triage validation; emergency alert; live position updates; join
  window enablement.
- **E2E:** full Pronto Atendimento journey (queue → room → summary with document) driven
  via SPEC-0018; scheduled teleconsultation join at window opening.

## Acceptance Criteria

- **AC1** (BR5, BR6) — Given triage complete and term accepted, when I enter the queue,
  then I see my position decreasing until called, without reloading the page.
- **AC2** (BR5) — Given I confirm "Sair da fila", then the session becomes `ABANDONADA`
  and the hub reopens.
- **AC3** (BR8) — Given I was called and do not respond within 5 minutes, then the session
  expires as `ABANDONADA` and I am notified (error case).
- **AC4** (BR9, BR10) — Given a closed session with an issued prescription, then the
  document appears in Minha Saúde for the attended beneficiary, dated today, and I receive
  the notification with the link.
- **AC5** (BR7) — Given I am already queued and try to start a new Pronto Atendimento,
  then the system resumes the existing session keeping my position.
- **AC6** (BR14) — Given a teleconsultation scheduled today at 15:00, then "Entrar na
  consulta" enables at 14:50 and the commitment shows the Telemedicina badge.
- **AC7** (BR1) — Given the instability notice is active, then the alert banner appears on
  the hub and the queue and I can still enter the queue.
- **AC8** (BR13) — Given MARIA starts Pronto Atendimento with PEDRO (minor in the test
  scenario) as active beneficiary, then the session is created for PEDRO with MARIA as
  author.

## Resolved Decisions (Phase 4 — owner)

- **OQ1 → state-driven room, no real media** (owner). The room is a state screen
  (professional + CRM, start time, running duration, states, "Encerrar minha participação")
  with **no audio/video**; real media is a post-POC evolution behind an ADR (ADR-0015).
- **OQ2 → 2-minute disconnection hold** (DL-0017). A session stays `EM_FILA` holding its
  position for up to 2 minutes of disconnection before it may expire.
- **Queue transport → SSE (push)** (owner, BR6). Position/ETA/state are pushed over
  Server-Sent Events (`text/event-stream`); the server re-emits the recomputed state
  periodically — no client polling, no broker (ADR-0016, DL-0022).
- **Professional side → minimal SPEC-0018 tele slice** (owner). The operator transitions
  (start attending, close with documents) are delivered in Phase 4 as a narrow, flag-gated
  slice of SPEC-0018 (start-attending, close-with-documents, issue-document) — the rest of
  SPEC-0018 lands in Phase 5 and absorbs it (ADR-0017, DL-0021).

## Out of Scope

Professional console (operator side — SPEC-0018 simulates); controlled-substance
prescriptions; life-risk urgency handling (triage directs to ER); WhatsApp/SMS alerts.
