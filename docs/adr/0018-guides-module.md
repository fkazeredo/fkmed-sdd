# ADR 0018: Module map revision — `domain.guides` (authorization guides + attendance token)

## Status

Accepted

## Context

SPEC-0012 introduces the beneficiary's **authorization guides** (opened by providers/operator; the
beneficiary only follows them) and the **attendance token** (a short-lived 6-digit antifraud code,
one valid per beneficiary). Guides carry a state machine whose status derives from per-item statuses,
notify the beneficiary on transitions (SPEC-0004), and are moved operator-side through the SPEC-0018
sim. The token is a per-beneficiary single-valid credential with a 10-minute TTL. This is a new
bounded context (guide custody/read + token issuance) that does not belong to `domain.appointment`,
`domain.clinicaldocs` or `domain.plan`. ADR-0001's "grow the map one spec at a time" policy is
triggered, and ADR-0017's revision trigger ("the full SPEC-0018 lands in Phase 5, extending the seam")
fires for the guide actions.

## Decision

Add **`domain.guides`** as the 12th verified Modulith module owning `guide` (number, type
`CONSULTA|SP_SADT|INTERNACAO`, beneficiary, requesting provider, requested_at, status, auth password +
validity, denial reason, optimistic `@Version`), `guide_item` (TUSS, description, quantity, item
status — the guide status **derives** from these) and `attendance_token` (code, beneficiary,
generated/expires/invalidated_at, created_by). The single-valid-token invariant is enforced by a
**partial unique index** on `beneficiary_id WHERE invalidated_at IS NULL` (generating a new token
invalidates the previous first — mirroring `domain.telemedicine`'s single-active session index).
Public API is read-only for beneficiaries: `GET /api/guides` (status/period filters, family-scoped),
`GET /api/guides/{id}`, `POST /api/tokens` (invalidates previous), `GET /api/tokens/current`.

Guides are moved **only** through an internal transition path the **operator-sim** calls — the
`/api/sim/guides/*` actions (create / authorize / partially-authorize / deny / cancel / mark-executed)
**extend the SPEC-0018 seam of ADR-0017** (same flag-gate + `OPERATOR_SIM` role + audit +
`sim.invalid-transition` 409), never via a beneficiary write path. Each transition publishes
`GuideStatusChanged`, consumed by `domain.notification` via a new `@ApplicationModuleListener`
(BR8 — number + status + denial reason only, never clinical/item detail). `GuideStatus`,
`GuideItemStatus`, `GuideType` are enums (state machines / fixed lists per DECISIONS-BASELINE §0019,
keep-criterion in their Javadoc); `GuidePeriod` is a fixed named-range filter. Family scope + the
dependent-token-generation audit reuse the `plan`/`BeneficiaryAccess` + audit patterns (SPEC-0003).

## Consequences

- **Positive:** a self-contained guides/token context feeding the Guias e Token screen; read-only
  surface for beneficiaries; the token single-validity and the sim seam reuse proven patterns
  (partial unique index; ADR-0017 operator-sim); no new notification infrastructure (the existing
  event→listener mechanism is reused).
- **Negative:** a 12th module raises the ModularityTest/diagram surface; the operator-sim controller
  grows a second action family (guides, after tele) — kept cohesive by delegating to the owning
  module's service.

## Alternatives Considered

- Fold guides into `domain.appointment` — rejected: a guide is an authorization artifact with its own
  lifecycle/owner, independent of a scheduled appointment.
- A dedicated guide notification type table — rejected (Rule Zero): the existing notification
  type/registry mechanism carries the guide status-change notification without a new structure.

## Revision Triggers

- Provider-side token validation (deferred, out of scope now) would add a write/validation surface.
- The finance sim actions (SPEC-0013, Phase 5 slice 5.2) will extend the same operator-sim seam and
  warrant the consolidated "Operator Simulation full API" ADR then.

## References

SPEC-0012 · SPEC-0018 (operator-sim guide actions) · SPEC-0004 (notifications) · SPEC-0003 (scope +
audit) · ADR-0001 (module map) · ADR-0017 (operator-sim seam, extended here) · ADR-0014
(telemedicine single-active index pattern) · DECISIONS-BASELINE §0019 · migration `V23__guides.sql` ·
diagram `docs/architecture-diagrams/modules.puml`.
