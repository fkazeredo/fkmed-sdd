# DL-0023 — Antifraud section: persisted title/message, static frontend guidance (SPEC-0014 BR3)

- **Phase/slice:** Phase 5 · 5.3 Atendimento (SPEC-0014)
- **Spec(s):** SPEC-0014 (BR2/BR3)
- **Related ADR:** ADR-0021 (domain.support)
- **Date:** 2026-07-07
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0014's §Persistence Changes lists `support_channel`, `faq_entry` and `libras_request` but
never mentions a table for the antifraud section's content, even though §Business Context frames
channel data, antifraud copy and FAQ entries together as "operator-managed content... the single
source of truth for what the portal displays." BR3 also requires 3 fixed best-practice bullets and
a link to the SPEC-0013 validator, which are not natural candidates for a content row either.

## Decision

Add a single-row `support_antifraud` table (`title`, `message`) as genuine operator content,
seeded alongside the other SPEC-0014 seeds. The 3 best-practice bullets and the validator link stay
as static frontend copy/structure (i18n) — BR2 forbids hardcoded **contact** information (phones,
URLs), not general safety guidance text, so this split does not violate it.

## Justification

Splitting lets the two genuinely-variable operator facts (title/message) live in content the
operator can change without a deploy, while avoiding a table for static navigational structure that
never varies (Rule Zero — no persistence for content that is really UI copy).

## Alternatives discarded

- No table at all, title/message also static frontend copy — rejected: contradicts §Business
  Context's explicit framing of antifraud copy as operator-managed content.
- A generic `content_block` table also covering the bullets — rejected: over-generalizes 3 fixed,
  never-reordered bullet strings into content-management machinery nothing else in the product needs
  (Rule Zero).

## Impact / How to revert

`support_antifraud` (single row) + `SupportService#antifraud()`/`AntifraudView`. Adding
operator-editable bullets later would mean a small ordered child table — additive, no migration of
existing data needed.
