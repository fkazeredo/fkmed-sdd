# DL-0020 — CID (diagnosis code) IS displayed on sick notes (owner decision)

- **Phase/slice:** Phase 4 · Clinical Documents (SPEC-0011 OQ1)
- **Spec(s):** SPEC-0011 (OQ1, BR6)
- **Related ADR:** ADR-0013 (domain.clinicaldocs)
- **Date:** 2026-07-06
- **Status:** OWNER-DECIDED
- **Confidence:** High
- **Reversibility:** Cheap

## Gap

SPEC-0011 OQ1: should sick notes display the CID (diagnosis code)? The spec proposed **not
displayed** (medical-privacy default). This is a product/legal call — not the agent's to assume.

## Decision

**Owner decided (AskUserQuestion): the CID IS displayed** on sick notes — in the detail screen and
in the PDF — overriding the spec's proposed default. The spec (§Resolved Decisions, BR6, §Persistence)
was updated to record the sick-note `cid` field and its display.

## Justification

Owner request is the top of the authority order (invariant 2). Recorded here (not silently applied)
because it reverses the spec's stated privacy default; the owner accepts the trade-off and can
revisit with legal.

## Alternatives discarded

- Keep the spec's "not displayed" default — rejected: the owner explicitly chose to display it.

## Impact / How to revert

A `cid` field surfaced on the sick-note detail + PDF. Reversible by hiding the field if the product
position changes; the stored data is unaffected.
