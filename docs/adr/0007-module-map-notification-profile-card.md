# ADR 0007: Module map revision — `domain.notification`, `domain.profile`, `domain.card` (Phase 2)

## Status

Proposed

## Context

Phase 2 ("Minha conta e identificação") delivers three new business capabilities at once —
SPEC-0004 (Notifications), SPEC-0006 (Profile and Account) and SPEC-0007 (Digital Card) — and
each introduces data ownership and rules that do not belong to any existing module. ADR-0001
established the verified Modulith map and its "grow deliberately, one spec at a time" policy;
its Revision Triggers name exactly this situation ("Any new spec introducing a business
capability… enters this map only through their owning specs"). A concurrent slice (SPEC-0005
Home) adds `domain.content` via ADR-0006; this ADR is independent and covers the Phase-2 trio.
Two sub-decisions must be recorded so the boundaries stay honest under `ApplicationModules.verify()`:
where legal-document versioning lives, and whether the card (a read model over plan data) earns
its own module.

## Decision

We will add **three** verified Modulith modules under `com.fkmed.domain`
(`explicitly-annotated` strategy, layered layout preserved):

- **`domain.notification`** (SPEC-0004) — the product-wide notification mechanism. Owns
  `notification`, the `notification_event_type` **registry** (code, description, email_default,
  mandatory; seeded by migration — baseline §0019) and `notification_preference` (V9). Consumes
  domain events **in-process, AFTER_COMMIT**; e-mail leaves through the existing `MailSender`
  port (ADR-0004), never inside the business transaction (BR6). Recipient resolution routes a
  beneficiary-targeted event with no own account to the **titular's account** (SPEC-0004 OQ1,
  resolved) via `domain.plan`'s facade; in-app listing filters to 12 months without hard delete
  (OQ2, resolved). Depends on `domain.error` and reads `domain.plan`/`domain.identity` facades
  by id only.
- **`domain.profile`** (SPEC-0006) — the beneficiary's editable contact/address data and the
  `beneficiary_photo` (bytes in DB, per beneficiary). Publishes `ContactDataChanged`
  (AFTER_COMMIT), consumed by `domain.notification` as a mandatory security notice to the old
  and new e-mail. Audits changes through `domain.audit`. Depends on `domain.plan`'s facade
  (beneficiary) and `domain.error`.
- **`domain.card`** (SPEC-0007) — the read-only digital card: assembles the card + data sheet
  from `domain.plan`'s facade, exposes CNS in full **only** here and in the server-side PDF
  (ADR-0008), and records a sensitive-data audit entry when a titular views a dependent's card
  (BR7, via `domain.audit`). Owns no business table of its own beyond a possible additive/CNS
  seed column added to the plan/beneficiary schema (V11) where SPEC-0001's mass lacks it.

**Legal-document versioning stays in `domain.identity`** (not a new module and not `domain.profile`):
identity already owns `term_acceptance` (V3) and first-acceptance at registration (SPEC-0002 BR15),
so the versioned Terms/Privacy documents (`legal_document`, V10) and the re-acceptance query live
next to their acceptance record — no cross-module write of another module's table. `domain.profile`
links to that flow through identity's facade.

On this branch's basis the verified map is **seven** modules: `domain.plan`, `domain.error`,
`domain.identity`, `domain.audit`, `domain.notification`, `domain.profile`, `domain.card`
(asserted by `ModularityTest`, drawn in the drift-gated `docs/architecture-diagrams/modules.puml`).
Once the parallel `domain.content` (ADR-0006) merges to `develop`, the map is eight — the diagram
is regenerated at Phase-2 integration.

## Consequences

- **Positive:** each capability keeps its own boundary and data ownership; the sensitive-data
  surface (CNS) is isolated in one small read-only module; the notification seam every later spec
  needs (invoice, guide, reimbursement) exists from Phase 2 with the registry catalog seeded.
- **Negative:** three modules land in one phase — a larger reviewed map delta than a single-spec
  slice, and the `modules.puml` diagram conflicts with the concurrent `domain.content` change at
  merge (trivial regenerate). `domain.notification` reading two facades (plan, identity) for
  recipient resolution is a deliberate, id-only coupling.
- Folding legal into `domain.identity` slightly grows that module, but avoids a fourth thin module
  and a cross-module table write (Rule Zero).

## Alternatives Considered

- **A dedicated `domain.legal` module for Terms/Privacy** — rejected: acceptance already lives in
  `domain.identity` (`term_acceptance`); a separate module would either duplicate the acceptance
  record or write another module's table, both worse than extending identity (Rule Zero).
- **Folding the card into `domain.plan`** (it is a read model over plan data) — rejected: the card
  carries its own concerns absent from the plan contract — full-CNS exposure limited to one screen,
  a dependent-view audit rule, and PDF generation — and putting them in `domain.plan` would bloat
  its bounded context and spread the sensitive-data surface.
- **Folding notifications into `domain.identity`** (the first producer is a security notice) —
  rejected: notifications are cross-cutting infrastructure every future module produces into, not
  an identity concern; coupling the mechanism to identity would force every later spec to reach
  through identity.

## Revision Triggers

- A beneficiary-targeted notification producer (SPEC-0012/0013/0016) exercises the titular-routing
  resolver beyond Phase 2's account-targeted events — revisit the resolver's placement.
- Legal-document content outgrows a seeded table (a CMS/authoring need) — reopen its placement.
- A cross-module dependency `verify()` rejects, forcing a boundary redesign.

## References

SPEC-0004 · SPEC-0006 · SPEC-0007 · ADR-0001 (module map) · ADR-0004 (Mailpit `MailSender` port) ·
ADR-0006 (parallel `domain.content`) · ADR-0008 (server-side card PDF) ·
DECISIONS-BASELINE §0001/§0010/§0012/§0019 · `docs/architecture/modules-and-apis.md`.
