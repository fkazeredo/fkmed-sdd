# ADR 0015: Telemedicine room is state-driven (no real audio/video) in the POC

## Status

Proposed

## Context

SPEC-0010 OQ1 asks whether the Pronto Atendimento / teleconsultation room needs real audio/video
(WebRTC) or a state-driven room screen. Real media is a large cost/architecture commitment
(signaling server, TURN/STUN, media permissions, NAT traversal, recording/compliance) far beyond a
POC's purpose, which is to demonstrate the beneficiary journey and state.

## Decision

**Owner decision:** the room is **state-driven, with no real media**. The room screen shows the
professional (name + CRM), the start time, a running duration, the session states and "Encerrar
minha participação" — driven by the session state machine and the SSE stream. The professional side
(start attending, close) is simulated via the SPEC-0018 tele slice (ADR-0017). Real audio/video is a
**post-POC evolution** to be introduced behind its own future ADR.

## Consequences

- **Positive:** the full journey (queue → room → closure → documents) is demonstrable with zero
  media infrastructure; keeps Phase 4 focused on the state/notification/document behavior that is the
  real system value; Rule Zero.
- **Negative:** the room does not exercise media; a later WebRTC introduction is a genuine new
  workstream (not a refactor of this screen).

## Alternatives Considered

- WebRTC now — rejected: disproportionate infrastructure and compliance surface for a POC.
- An embedded third-party video widget — rejected: external dependency + data-sharing/CSP surface
  with no POC benefit.

## Revision Triggers

- A decision to pilot real remote care ⇒ a new ADR introducing WebRTC/media behind the same room
  states.

## References

SPEC-0010 (OQ1, BR9) · ADR-0014 (telemedicine module) · ADR-0016 (SSE) · ADR-0017 (operator-sim).
