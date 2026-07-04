# 0018 - Operator Simulation API

**Status:** Draft

## Goal

Make the POC demonstrable end-to-end without an operator back office: a **dev-only,
internal-role REST API** drives the operator-side transitions (authorize guides, approve
and pay reimbursements, conclude previews, conduct telemedicine sessions, issue documents,
generate invoices) — producing exactly the same events, notifications and timeline entries
a real back office would, so beneficiary-facing modules cannot tell the difference.

## Scope

- Profile/flag-gated REST endpoints (no UI) for operator actions across modules.
- Internal role and dev-seeded operator credential.
- Guard rails: state machines respected, full auditing, idempotent payment execution.

## Business Context

The drafts declare the operator "has no interface in this POC", yet every flow depends on
operator actions to move state. Owner decision: the **analysis engine is real code**
(SPEC-0016 BR2/BR3); everything else operator-side is simulated through this API — the
explicit, traceable seam prescribed by `docs/architecture/simulation-and-mocking.md`.
E2E tests and live demos are its consumers.

## Business Rules

- **BR1** — The API MUST be enabled only when the explicit configuration flag is on and
  MUST NEVER be active under the production profile (the prod fail-fast validator refuses
  the flag). Disabled ⇒ routes absent (404), not merely forbidden.
- **BR2** — Every call REQUIRES an authenticated **internal operator role**
  (`OPERATOR_SIM`), held by a dev-seeded operator credential — never by beneficiary
  accounts.
- **BR3** — Every action MUST be audited (SPEC-0003) with the operator as author, and MUST
  produce the same domain events, notifications and timeline entries as the equivalent
  real back-office action — consuming modules MUST NOT be able to distinguish the origin.
- **BR4** — Transitions MUST respect the owning specs' state machines (SPEC-0012 BR6,
  SPEC-0016 BR1, SPEC-0010 BR11); an invalid transition MUST be rejected with `409` and
  MUST NOT force state.
- **BR5** — Supported actions (one endpoint each, per owning spec):
  - **Guides (SPEC-0012):** create guide (type, beneficiary, provider, items); authorize /
    partially authorize (password + validity, per-item statuses) / deny (reason) / cancel /
    mark executed.
  - **Reimbursements (SPEC-0016):** approve (confirming engine calculation); deny
    (reason); open pendency (description); execute payment (success → `PAGO` | failure →
    `PAGAMENTO_NAO_EFETUADO`).
  - **Previews (SPEC-0017):** conclude analyzed preview (estimated value).
  - **Telemedicine (SPEC-0010):** start attending the next queued session (professional
    name + CRM); close a session, optionally issuing clinical documents.
  - **Clinical documents (SPEC-0011):** issue a document from the operator (any type).
  - **Finance (SPEC-0013):** generate an invoice for a competência (with digitable line +
    PIX code); mark an invoice paid; register a copay entry.
- **BR6** — Payment execution MUST be idempotent (repeating the call for an already-paid
  request does not double-pay nor duplicate events).
- **BR7** — The API is REST-only (no UI); requests/responses documented in the OpenAPI
  snapshot like any other contract.

## Input/Output Examples

- `POST /api/sim/guides/{id}/authorize` `{"password":"AUT-482913","validUntil":"…"}` →
  `200`; guide was `NEGADA` → `409 {"code":"sim.invalid-transition"}` (error case).
- `POST /api/sim/reimbursements/{id}/pay` `{"outcome":"FAILURE"}` → `200` request now
  `PAGAMENTO_NAO_EFETUADO` + beneficiary notified; repeat `{"outcome":"SUCCESS"}` twice →
  single `PAGO` transition (BR6).
- Any `/api/sim/**` with the flag off → `404` (error case).
- Any `/api/sim/**` as a beneficiary account → `403` (error case).

## API Contracts

`/api/sim/**` family per BR5. Shapes finalized at implementation; the OpenAPI snapshot
governs. All routes tagged `operator-simulation` and absent when disabled (BR1).

## Events

Publishes **no events of its own** — it invokes the owning modules' application services,
which publish their regular events (BR3).

## Persistence Changes

Dev-seeded operator credential (dev/e2e profiles only). No new business tables — actions
persist through the owning modules.

## Validation Rules

Payload validation delegates to the owning modules' rules (e.g. password format, reason
required on denial, invoice line = 47 digits). Flag + role checks on every route (BR1/BR2).

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Flag disabled | *(routes absent)* | 404 |
| Caller lacks `OPERATOR_SIM` | `sim.forbidden` | 403 |
| Transition not allowed by owning state machine | `sim.invalid-transition` | 409 |
| Target resource not found | `sim.target-not-found` | 404 |

## Observability Requirements

Every action logged as a business event (`sim` marker + target + transition) and audited
(BR3). Counter per action type — demo/E2E traffic visibility.

## Tests Required

- **Domain/unit:** not applicable beyond owning modules (this module has no own domain
  logic — Rule Zero).
- **Integration (Testcontainers):** each BR5 action drives the owning module correctly;
  BR4 rejections; BR6 idempotency; flag-off ⇒ 404; role enforcement.
- **API contract:** snapshot includes the `sim` routes (dev profile snapshot).
- **E2E:** used as the driver of other specs' journeys (guides authorization, reimbursement
  payment, telemedicine attendance) — its own E2E is that usage.

## Acceptance Criteria

- **AC1** (BR3) — Given the operator authorizes a seeded guide via the API, then the
  beneficiary receives the SPEC-0012 notification and the guide detail shows password and
  validity — indistinguishable from a real back-office action.
- **AC2** (BR4) — Given a `NEGADO` reimbursement, when the operator tries to pay it, then
  the call is rejected with `409` and no state changes (error case).
- **AC3** (BR6) — Given payment executed twice for the same request, then exactly one
  `PAGO` transition and one notification exist.
- **AC4** (BR1) — Given the production-like profile, then `/api/sim/**` routes do not
  exist and the startup validator refuses the enabled flag (error case).
- **AC5** (BR2) — Given a beneficiary account calling any sim route, then `403` (error
  case).

## Open Questions

*(none — scope fixed by the owner's decision: dev-only API + real analysis engine)*

## Out of Scope

Any operator UI/console (possible future); bulk/scenario scripting beyond single actions;
production use of any kind; replacing the real analysis engine (SPEC-0016 owns it).
