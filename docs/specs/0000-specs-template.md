# NNNN - Title

> **Template rules:** file name `NNNN-kebab-case-title.md` (NNNN = next sequential number).
> All sections are normative — fill every applicable one; when a section truly does not
> apply, write `Not applicable.` instead of deleting it, so reviewers know it was considered.
> A spec is a **living artifact**: update it in the same PR as the code it governs. Language:
> en-US. Never put anything here that the owner has not decided — undecided items go to
> **Open Questions**.

**Status:** Draft | Approved | Superseded by SPEC-NNNN

## Goal

> fill: one paragraph — the user/business outcome this slice delivers. Not the mechanism.
> Common mistake: describing the implementation instead of the value.

## Scope

> fill: bullet list of what IS in this slice. Keep it thin (vertical slice: migration →
> domain → API → screen). Anything doubtful goes to Out of Scope or Open Questions.

## Business Context

> fill: 2-5 sentences of domain background a newcomer needs to judge the rules below.
> Common mistake: novel-length context — link to the domain doc instead.

## Business Rules

> fill: numbered, testable, MUST/MUST NOT language. One rule = one obligation. Each BR must
> be referenceable by at least one test (traceability BR ↔ test). A rule the owner assumed
> autonomously is marked `ASSUMED (see DL-NNNN)`.

- **BR1** — The system MUST …
- **BR2** — When …, the system MUST NOT …

## Input/Output Examples

> fill: concrete request/response pairs that make the rules unambiguous — include at least
> one ERROR case. These examples become test fixtures.

## API Contracts

> fill: endpoints touched/created — method, path, request/response DTO shapes, status codes.
> The OpenAPI snapshot gate validates the implementation against reality.

## Events

> fill: domain events published/consumed (name, payload essentials, when fired —
> AFTER_COMMIT). `Not applicable.` if none.

## Persistence Changes

> fill: tables/columns/indexes + the Flyway migration number. Remember: reference data goes
> in the registry table, not enums (DECISIONS-BASELINE §0019); no cross-context FKs.

## Validation Rules

> fill: field-level constraints (required, formats, ranges) and where they are enforced.

## Error Behavior

> fill: each failure mode → `DomainException` code (== i18n key) → HTTP status. New codes
> need messages in every product locale (the completeness gate checks).

## Observability Requirements

> fill: business events to log (with masking of personal data), metrics/counters to expose.
> `Not applicable.` only for pure-UI slices.

## Tests Required

> fill: per layer — domain/unit, integration (Testcontainers), API contract, frontend unit,
> E2E journey. State explicitly which layers this slice does NOT reach, and why.

## Acceptance Criteria

> fill: executable Given/When/Then, each pointing at the BR(s) it proves. These become the
> developer's end-of-build tests (step 3 of the loop) and QA's homologação script (step 4).
> If a criterion can't be written as Given/When/Then, the
> rule above is not testable yet — fix the rule.

- **AC1** (BR1) — Given …, when …, then …
- **AC2** (BR2) — Given …, when …, then … (error case)

## Open Questions

> fill: everything that affects behavior/contracts/data/security and the owner has NOT
> decided. The `/slice` gate BLOCKS implementation while an OQ affecting the slice is open.
> When decided: move the answer into Business Rules (owner answer, or `ASSUMED (see
> DL-NNNN)` under authorized autonomy) and strike the OQ.

- **OQ1** — question · impact if we guess wrong · proposed default

## Out of Scope

> fill: explicit non-goals of this slice (prevents scope creep and "while we're here").
