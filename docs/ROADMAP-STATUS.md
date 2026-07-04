# ROADMAP-STATUS — execution log

Append-only execution log: **one line per closed slice**, written by `/dod`. Canonical
source order for status reports: this file → `release-notes/CHANGELOG.md` →
`decision-log/INDEX.md` → `git log` → `ROADMAP.md`. PR open ≠ merged ≠ released with tag —
never conflate (DECISIONS-BASELINE §0023).

| Date | Phase | Slice | Spec(s) | Branch / PR | Gates | E2E | Notes |
|---|---|---|---|---|---|---|---|
| 2026-07-04 | 0 | walking-skeleton | 0001 | `feature/walking-skeleton` / PR #2 (merged) | ✅ backend verify (83 tests, PIT 98%) · frontend lint/test/build (13 tests) — run 3× (dev, QA, /dod) | ✅ 3/3 isolated stack | v0.1.0 (tag pending, owner). QA APPROVED zero blockers; fresh-eyes rework (11 items) done. CI-triage: `mvnw` exec bit fixed (Windows-dropped, 4 jobs). Owner decisions: tenant_id removed → ADR-0003 (revises §0003, single-tenant per build); CSRF false-positive → ADR-0002 (owner to dismiss CodeQL alert). ADR-0001/0002/0003 Proposed |
| 2026-07-04 | 1 | 1.1 first-access-login | 0002, 0003 (subset) | `feature/first-access-login` / PR pending | ✅ backend verify (167 tests, PIT 90%/96%) · frontend lint/test/build (32 tests) — run by dev, QA (independent, APPROVED) and architect (re-verified post-rework) | ✅ 4/4 isolated stack (+ Mailpit) | v0.2.0. Real accounts replace the SPEC-0001 dev-login seam (MARIA seeded ACTIVE, PEDRO exercises first access). Fresh-eyes: approve with reservations → 14-item rework (1 spec gap: CPF check-digit; rest test-quality gaps) all closed with committed regressions. ADR-0004 (Mailpit email seam), DL-0001 (first-access contract shape). Slice covers 0002's first-access/verification/login BRs + 0003's audit-trail BR6/BR7/BR10 only — lockout/recovery/selector/protocol-generator are slices 1.2/1.3 |
