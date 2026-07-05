# ADR 0010: Module map revision — `domain.card` (Digital Card)

## Status

Proposed

## Context

SPEC-0007 (Digital Card) introduces the beneficiary's card + data sheet (`GET
/api/cards/{beneficiaryId}`) and its PDF download (`GET /api/cards/{beneficiaryId}/pdf`). This is a
new business capability with its own error state (`card.unavailable`, BR10) and its own sensitive-
data exception to BR8 (CNS shown in full only here) — it does not belong to `domain.identity`
(accounts/login) or `domain.audit` (the append-only trail). It reads existing `domain.plan` data
(beneficiary + plan registry, no new aggregate of its own) and reuses `domain.plan`'s existing
family-scope facade rather than reimplementing authorization (ADR-0001/DL-0004 already placed that
logic in `BeneficiaryAccess`). ADR-0001's Revision Triggers name exactly this case: "Any new spec
introducing a business capability... enters this map only through their owning specs."

## Decision

We will add **`domain.card`** as a sixth verified Modulith module (`explicitly-annotated` strategy,
consistent with the rest of the map). It owns no new tables (SPEC-0007 §Persistence Changes: reads
the existing `plan`/`beneficiary` registry, extended in place by `V9__card_registry.sql` with the
`plan.category` column — DL-0010). Its public facade is `CardService` (`@Service`), consumed only by
`application.api.CardController`. Cross-module dependencies:

- `domain.plan`'s `BeneficiaryAccess.cardDetailsFor(...)` — the family-scope decision (in/out of
  scope, active/inactive) AND the sensitive registry read (CNS in full, plan name/ANS/coverage/
  category/additives) come from ONE extended method on the existing SPEC-0003 scope facade, so
  scoping is reused, not reimplemented; `BeneficiaryAccess`'s existing methods
  (`accessibleFor`/`summaryFor`) are untouched.
- `domain.identity`'s `IdentityAccounts.findByEmail(...)` — resolves the acting account id for the
  BR7 audit entry, the same pattern `AuthenticationAuditListener`/`LogoutAuditRecorder` already use.
- `domain.audit`'s `AuditRecorder`/`AuditEventTypes` — records the dependent-card-view entry
  (`card.dependent-viewed`) in the same transaction as the read (BR4/BR7).

The verified map is now **six modules**: `domain.plan`, `domain.error`, `domain.identity`,
`domain.audit`, `domain.content`, `domain.card` (asserted by `ModularityTest`, drawn in the
drift-gated diagram `docs/architecture-diagrams/modules.puml`).

## Consequences

- **Positive:** the card feature's authorization and CNS-exposure logic live in exactly one place
  each (`BeneficiaryAccess` for scope, `domain.card` for the PDF/response shape), so there is no
  second, divergent implementation of family-scope rules anywhere in the codebase.
- **Positive:** `domain.card` owns zero new tables — the smallest possible footprint for a module
  that is fundamentally a read + rendering feature over data another module already owns.
- **Negative:** `BeneficiaryAccess` now has one method (`cardDetailsFor`) that is a deliberate,
  documented exception to its own "never CPF/CNS" posture (its other methods still never expose
  it) — a future reader must check the Javadoc, not just the class-level summary, to know which
  method is safe to reuse for a non-card feature.
- **Negative:** `domain.card` depends on three other modules (`plan`, `identity`, `audit`) — the
  widest fan-in of any module so far; acceptable here because none of the three depend back on
  `card` (no cycle) and each dependency is a single, narrow, already-existing public method.

## Alternatives Considered

- **Folding the card endpoints into `domain.plan`** (it already owns the beneficiary/plan data) —
  rejected: the card has its own error vocabulary (`card.unavailable`), its own delivery shape (PDF
  rendering) and its own audit obligation (BR7) that are conceptually a distinct capability from
  "the beneficiary/plan contract"; growing `domain.plan` for every reader of its data would make it
  a dumping ground (the same reasoning ADR-0006 used to keep `domain.content` separate).
- **Returning CNS/plan fields from a brand-new `domain.plan` facade parallel to `BeneficiaryAccess`,
  with `domain.card` re-deriving the in-scope/active decision itself** — rejected: it would either
  duplicate the family-walk logic (a second implementation of "who is in this caller's scope" to
  keep in sync) or require two round trips to the plan module for one read; a single extended
  method on the existing facade is the smaller change (Rule Zero) and keeps the "reuse the scope
  facade" instruction from the slice plan literally true.
- **A generic PDF-rendering infra port** (`infra.pdf` or similar technical adapter) — rejected as
  premature: OpenPDF is a pure, deterministic, no-I/O library call (no external system, no
  mock-vs-real seam per baseline §0006), so a port/adapter pair would be architecture theater ahead
  of a second consumer (Rule Zero).

## Revision Triggers

- A second document-generation need (e.g. a reimbursement PDF, SPEC-0015/0017) appears — worth
  reconsidering whether PDF rendering deserves a small shared utility (still not a port, unless a
  real swappable backend shows up).
- `domain.card` grows its own persisted state (e.g. a PDF-download counter beyond a simple metric) —
  would need its own migration and a revisit of the "zero tables" framing above.

## References

SPEC-0007 (Digital Card) BR1-BR10 · ADR-0001 (Initial Modulith Module Map — revised by this ADR) ·
ADR-0006 (`domain.content`, the same "grow deliberately, one spec at a time" precedent) · DL-0004
(`BeneficiaryAccess` placement) · DL-0010 (`planCategory` seed value) · DECISIONS-BASELINE
§0001/§0010/§0012/§0016/§0019 · `docs/architecture/modules-and-apis.md` · diagram snapshot
`docs/architecture-diagrams/modules.puml` · `backend/src/main/java/com/fkmed/domain/card/`.
