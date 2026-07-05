# DL-0009 — Digital-card PDF layout (SPEC-0007 OQ1)

- **Phase/slice:** Phase 2 · Digital Card (SPEC-0007)
- **Spec(s):** SPEC-0007 (BR3; Open Question OQ1)
- **Related ADR:** ADR-0007 (PDF generation library)
- **Date:** 2026-07-05
- **Status:** ASSUMED
- **Confidence:** High
- **Reversibility:** Cheap

## Gap

SPEC-0007 OQ1 leaves the PDF layout open — a portrait full-page form versus a card-sized page.
Pure presentation, cheap to change.

## Decision

Lay the **card on an A4 page**: the front visual (product brand, beneficiary name, plan category
and name, card number, coverage seal) followed by a data block (CNS, ANS registration, coverage,
additives) — all BR3 fields, with the card's visual identity.

## Justification

A4 prints on any reception-desk printer; card-on-A4 preserves the card's visual identity (what
BR3 asks) while fitting standard paper. Trivially adjustable later.

## Alternatives discarded

- **Card-sized page** — rejected: awkward to print on standard office printers.
- **Portrait full-page form** — rejected: loses the card visual identity BR3 requires.

## Impact

- Files: the PDF service layout only (OpenPDF, ADR-0007). No data/model impact.

## How to revert

Change the layout in the PDF service; no data migration involved.
