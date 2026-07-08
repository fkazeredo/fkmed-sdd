# ADR 0016: Server-Sent Events (SSE) for the telemedicine queue's near-real-time state

## Status

Accepted

## Context

SPEC-0010 BR6 requires the queue position/ETA to update in near real time without user action. The
owner chose **push** (SSE) over client polling. The backend is a Spring MVC (servlet) stack with no
WebSocket/message-broker infrastructure today. This is the first streaming surface in the codebase.

## Decision

Expose the current-session read as a **Server-Sent Events** stream (`text/event-stream`) via Spring
`SseEmitter`: `GET /api/tele/sessions/current` returns the live `{state, position, etaMinutes, room…}`
and the server **re-emits the recomputed state on a short fixed cadence** while the emitter is open,
plus an immediate emit on the beneficiary's own transitions (turn reached, closed) — DL-0022. Emitters
are registered per session and cleaned up on completion/timeout/disconnect; reconnection within the
2-minute hold (DL-0017) resumes the same session/position. No message broker, no WebSocket, no client
polling. The same payload shape is available as a plain JSON GET for non-streaming clients/tests.

## Consequences

- **Positive:** the "push" UX with zero new infrastructure (SSE is built into the servlet stack);
  graceful fallback to a plain GET; simple to test.
- **Negative:** long-lived connections need emitter lifecycle discipline (leaks/timeouts); periodic
  re-emit is server-side polling in disguise (acceptable for a POC queue); horizontal scaling would
  need sticky sessions or a shared emit source (out of scope now).

## Alternatives Considered

- Client polling — rejected: the owner chose push.
- WebSocket + STOMP + broker (Redis) — rejected: heavy new infrastructure for one queue screen
  (Rule Zero); revisit only if load/fan-out demands it.

## Revision Triggers

- Multi-instance scaling or high fan-out ⇒ a broker-backed push (new ADR).
- Real event-driven push (emit only on actual queue mutations) if the fixed cadence proves wasteful.

## References

SPEC-0010 (BR6) · ADR-0014 (telemedicine module) · DL-0022 (SSE design) · DL-0017 (reconnection hold)
· `docs/architecture/messaging-and-integrations.md` · `docs/architecture/observability.md`.
