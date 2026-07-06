# DL-0022 — SSE queue transport: periodic server re-emit (SPEC-0010 BR6)

- **Phase/slice:** Phase 4 · Telemedicine (SPEC-0010 BR6)
- **Spec(s):** SPEC-0010 (BR6)
- **Related ADR:** ADR-0016 (SSE transport)
- **Date:** 2026-07-06
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

The owner chose **SSE (push)** for the queue's near-real-time position/ETA (over polling). How does
the server decide when to push, given there is no message broker and the servlet stack is MVC?

## Decision

`GET /api/tele/sessions/current` streams `text/event-stream` via a Spring `SseEmitter`. The server
**re-emits the recomputed session state (state, position, ETA) on a short fixed cadence** (a few
seconds) while the emitter is open, plus an immediate emit on the beneficiary's own transitions
(turn reached, closed). No broker, no client polling. Emitters are tracked per session and cleaned
up on completion/timeout/disconnect; the 2-minute hold (DL-0017) governs reconnection.

## Justification

Gives the "push" feel the owner wants without introducing streaming infrastructure the codebase does
not have (WebSocket/broker). Periodic re-emit is simple, stateless-enough, and correct for a queue
whose position changes are driven by the operator-sim; revisit to event-driven push if load demands.

## Alternatives discarded

- Client polling — the owner explicitly chose push over this.
- WebSocket + broker (STOMP/Redis) — rejected: heavy new infra for a POC queue (Rule Zero).

## Impact / How to revert

One SSE endpoint + an emitter registry + a scheduled re-emit. Reversible to polling by swapping the
endpoint to a plain GET; the client contract (state/position/eta) is the same shape.
