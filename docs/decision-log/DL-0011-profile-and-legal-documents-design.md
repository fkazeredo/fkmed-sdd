# DL-0011 — Profile & legal-document design decisions (SPEC-0006 backend)

- **Phase/slice:** Phase 2 (Profile & Account) — backend sub-branch `feature/phase-2-profile--be`
- **Spec(s):** SPEC-0006 (BR2, BR3, BR5, BR6, BR7, BR8; §API Contracts; §Persistence Changes)
- **Related ADR:** none (no new Modulith module — see Decision 1)
- **Date:** 2026-07-05
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

The architect froze the API contract and reserved migrations V11/V12, and the work order
delegated a set of design calls explicitly ("your call"): where the legal-document versioning
lives, how the two document code-spaces reconcile, how "current version" is determined, and
whether the UF registry needs caching. SPEC-0006 has no Open Questions (photo moderation was
explicitly deferred), so these are implementation-detail gaps inside the frozen contract, not
spec-level questions — resolved here under the work order's delegated latitude.

## Decision

1. **No new Modulith module.** Contacts/photo extend `domain.plan` (which already owns the
   beneficiary/family model); legal-document versioning + acceptance extend `domain.identity`
   (which already owns `term_acceptance`, `LegalDocumentTypes` and the terms/privacy version
   config). This keeps the verified module map at exactly the 7 modules `ModularityTest`
   asserts and needs no ADR (ADR-0011 was pre-reserved only *if* a module were added).
2. **Two legal code-spaces bridged by one mapper.** The public API/catalogue codes are
   `TERMS`/`PRIVACY` (on the `/api/legal-documents` paths and the `legal_document.type`
   column, per the frozen contract); the acceptance codes stay `TERMS_OF_USE`/`PRIVACY_POLICY`
   in `term_acceptance` (as SPEC-0002 BR15 first access already writes them).
   `LegalDocumentTypes.acceptanceCodeFor` maps API → acceptance so first-access acceptances and
   SPEC-0006 portal re-acceptances share **one** immutable history.
3. **"Current version" = latest `published_at` per type;** seeded at `1.0`/`1.0` to match the
   existing `app.legal.terms-version`/`privacy-version` defaults recorded at first access, so a
   freshly-registered user is already up to date (no spurious interception). Superseding a
   version inserts a new `legal_document` row (history preserved).
4. **UF registry validated but not cached.** UF validation runs only on the low-frequency
   contact save, so a Caffeine cache (DECISIONS-BASELINE §0022 pattern) would add moving parts
   without solving a hot-path problem (Rule Zero). A plain `existsById` against the seeded
   `uf_registry` table is used.
5. **Contacts modeled as an `@Embedded` value object** on the `beneficiary` row (columns, not a
   1:1 `beneficiary_contact` table): one aggregate, no join, mutation only through the
   `Beneficiary.updateContacts` business method (no setters — ArchUnit-safe). Photo is a
   separate `beneficiary_photo` table (bytes must not load with every beneficiary fetch).

Contract details filled where the frozen contract was silent (reported to the architect for
frontend sync, not silent changes): `POST /accept` carries `{ "version": "…" }` (required to
detect the 409 outdated case); an **additive** `GET /api/legal-documents/{type}` serves the page
body (BR8 needs the text, and the frozen `GET /current` deliberately omits it); a new
`profile.landline-invalid` (422) code for the landline format the spec specifies without an error
code; `spring.servlet.multipart.max-file-size=10MB` so a >5 MB photo reaches the domain and gets
`422 profile.photo-too-large` instead of the container's 413.

## Justification

The module-placement and code-space decisions follow the work order's explicit delegation and
Rule Zero (reuse the existing acceptance area rather than a cross-module port for a two-table
concern). Seeding `1.0` off the existing `app.legal` defaults keeps the identity first-access
flow correct without touching it. The additive endpoints/fields are the minimum needed for
higher-authority spec rules (BR8 body display, BR2 5 MB boundary) to function; each is
non-breaking (adds a read endpoint / an optional-in-practice request field) and flagged for the
architect to re-sync the parallel frontend.

## Alternatives discarded

- **A new `domain.legal` module** — rejected: it would break `ModularityTest`'s exact 7-module
  assertion, force a cross-module port to write `term_acceptance` (identity-internal), and need
  an ADR — cost with no boundary benefit for a two-table concern (Rule Zero).
- **`legal_document.type` = `TERMS_OF_USE`/`PRIVACY_POLICY`** (single code-space) — rejected: the
  frozen contract names the API/table type `TERMS`/`PRIVACY`; mapping at the acceptance write is
  cheaper than a contract change.
- **Caching the UF registry** — rejected as premature (Rule Zero); revisit only if UF validation
  ever becomes a hot path.
- **A `beneficiary_contact` 1:1 table** — rejected: an embedded VO is simpler (no join, no extra
  lifecycle) for 10 small string columns.

## Impact

- Specs: SPEC-0006 unchanged (frozen contract honoured; these are implementation details). No
  Open Question to move (spec has none).
- Migrations: `V11__profile_contacts_photo.sql` (contact columns + `uf_registry` + seed +
  `beneficiary_photo`), `V12__legal_documents.sql` (`legal_document` + seed + `term_acceptance`
  uniqueness + MARIA acceptance seed).
- Contract: OpenAPI snapshot regenerated with the 6 endpoints; the `GET /{type}` body endpoint
  and the `accept` `version` field are additions the architect must reflect to the frontend.
- Module map: new `plan → audit` edge (contact/photo audit) — diagram regenerated; the 7-module
  map is unchanged.

## How to revert

Module placement and the code-space mapper are internal — reversible by moving the classes and
changing `acceptanceCodeFor` (no data migration; `term_acceptance` codes are unchanged). Dropping
the additive `GET /{type}` endpoint or the `accept` `version` field is a contract change to
coordinate with the frontend. Adding UF caching later is a follow-up, not a revision of this DL.
