# ADR 0017: Operator-simulation seam — the telemedicine+documents slice of SPEC-0018 (Phase 4)

## Status

Proposed

## Context

SPEC-0010's journey (queue advances → room → closure issuing documents) and SPEC-0011's tele-born
documents are **driven by the operator side**, which SPEC-0018 (Operator Simulation API) owns. The
ROADMAP schedules the full SPEC-0018 in Phase 5, but the Phase 4 journey and its end-to-end E2E
cannot complete without a driver. The owner decided (DL-0021) to bring only the **narrow tele+docs
slice** now, leaving the rest of SPEC-0018 for Phase 5.

## Decision

Introduce the operator-simulation seam with only its **telemedicine + clinical-document actions**:
`POST /api/sim/tele/sessions/next/start` (start attending the next queued session — professional
name + CRM), `POST /api/sim/tele/sessions/{id}/close` (close, optionally issuing clinical documents),
`POST /api/sim/documents` (issue a document from the operator). It lives as an **application-layer
adapter** calling the `domain.telemedicine` and `domain.clinicaldocs` facades — not a new domain
module. It carries SPEC-0018's guard rails from day one:

- **Config-flag gated** — routes exist only when the explicit flag is on; **absent/404 when off**.
- **Internal operator role** — a dev-seeded operator credential; **403 for beneficiary accounts**.
- **Audited** with the operator as author; state-machine transitions respected (invalid → 409).
- **Prod fail-fast** — the startup validator refuses the enabled flag under a production-like profile
  (same pattern as the dev-credential allowlist, invariant 9).

The full SPEC-0018 (guides, reimbursements, invoices, previews) lands in Phase 5 and **extends the
same seam/routes** — this slice is forward-compatible, not throwaway.

## Consequences

- **Positive:** unlocks the complete Phase 4 journey and E2E (AC1/AC3/AC4) with a minimal, guarded,
  forward-compatible surface; no beneficiary can reach it; disabled in prod.
- **Negative:** an early `/api/sim/**` family exists before the full SPEC-0018 spec is realized —
  mitigated by keeping it to exactly the tele+doc actions (Rule Zero) and gating it hard.

## Alternatives Considered

- Defer all of SPEC-0018 — rejected by the owner: no driver ⇒ the journey/E2E cannot run.
- A test-only hook (no HTTP) — rejected: the E2E runs over HTTP against the real stack and needs a
  real (guarded) trigger; a hidden test hook would not exercise the true path.
- Build full SPEC-0018 now — rejected: pulls unrelated scope forward with no consumer.

## Revision Triggers

- Phase 5 realizes the full SPEC-0018 — this ADR is superseded/extended by the SPEC-0018 module ADR.

## References

SPEC-0018 (BR1–BR5) · SPEC-0010 (BR9/BR10) · SPEC-0011 (issuance) · DL-0021 (slice scope) · ADR-0014
(telemedicine) · ADR-0013 (clinicaldocs) · invariant 9 (dev-only, prod fail-fast) ·
`docs/architecture/simulation-and-mocking.md` · `docs/architecture/security.md`.
