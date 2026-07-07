# ADR 0020: Operator Simulation full API — guides + finance actions (extends ADR-0017)

## Status

Proposed

## Context

ADR-0017 introduced the operator-simulation seam with only its **telemedicine + clinical-document**
actions (Phase 4), explicitly forward-compatible and with the revision trigger "Phase 5 realizes the
full SPEC-0018 — this ADR is superseded/extended by the SPEC-0018 module ADR." Phase 5 now adds the
**guides** actions (slice 5.1, ADR-0018) and the **finance** actions (slice 5.2), completing the
Phase-5 scope of SPEC-0018. This ADR records the consolidated seam and **supersedes/extends
ADR-0017**.

## Decision

Keep the operator-simulation as a single **application-layer adapter** (`application.sim`,
`OperatorSimulationController` + `SimService`) — NOT a domain module (Rule Zero) — calling the owning
modules' facades and extended, over Phases 4–5, with these action families under `/api/sim/**`:

- **Telemedicine + documents** (ADR-0017): `/api/sim/tele/*`, `/api/sim/documents`.
- **Guides** (SPEC-0012, slice 5.1): `/api/sim/guides` + `/{id}/authorize|partially-authorize|deny|
  cancel|mark-executed` — drives the guide state machine, publishes `GuideStatusChanged`.
- **Finance** (SPEC-0013, slice 5.2): `/api/sim/finance/invoices` (generate → `InvoiceIssued`),
  `/{id}/pay` (**idempotent** — BR6, repeat does not double-pay nor duplicate events),
  `/api/sim/finance/copay`.

Every action keeps SPEC-0018's guard rails from ADR-0017 unchanged: **config-flag gated** (routes
absent/404 when off), **internal `OPERATOR_SIM` role** (403 for beneficiaries), **audited** with the
operator as author, **owning state machines respected** (invalid → 409 `sim.invalid-transition`,
missing → 404 `sim.target-not-found`), **prod fail-fast**. The seam returns delivery-safe view
records (never owning entities) — e.g. `GuideTransitionResult`, `SimGuideResult`, `InvoiceIssuedResult`.

**Out of Phase 5 scope (still deferred):** the reimbursement and preview sim actions of SPEC-0018 BR5
belong to **Phase 6** (specs 0015–0017) and are not built here.

## Consequences

- **Positive:** one guarded, cohesive `/api/sim/**` family drives every operator-side transition the
  POC needs (tele, docs, guides, finance) with identical events/notifications/audit as a real back
  office — E2E journeys and demos have a real, guarded trigger; no beneficiary can reach it; disabled
  in prod.
- **Negative:** the sim controller/service grows several action families — kept cohesive by
  delegating to the owning modules' facades and returning view records, never entities.

## Alternatives Considered

- A separate sim module per owning context — rejected (Rule Zero): the sim has no domain logic of
  its own; it is a thin adapter.
- Test-only hooks — rejected (as in ADR-0017): the E2E runs over HTTP against the real stack.

## Revision Triggers

- Phase 6 adds the reimbursement/preview sim actions (specs 0015–0017) — a further extension of this
  same seam.

## References

SPEC-0018 (BR1–BR7) · ADR-0017 (**superseded/extended** — tele+docs slice) · ADR-0018 (guides) ·
ADR-0019 (finance) · DL-0021 (tele slice scope) · invariant 9 (dev-only, prod fail-fast) ·
`docs/architecture/simulation-and-mocking.md` · `docs/architecture/security.md`.
