---
description: >
  Creates a new ADR in docs/adr from the official template, with a sequential number and the
  index updated. Use when a decision affects architecture: structure, stack, a major
  dependency, a module boundary, persistence/messaging, security, or anything costly to
  reverse (criteria in docs/architecture/workflow.md). Keywords: ADR, decisão arquitetural,
  architecture decision.
argument-hint: <kebab-case-title> [decision context]
allowed-tools: Read, Write, Edit, Glob, Grep
---

# /adr — create an architecture decision record

Create an ADR following the project's method. All conversation with the owner is in **pt-BR**.

## Steps

1. **Entry gate (Rule Zero):** before creating, check the "when an ADR is justified" criteria
   in `docs/architecture/workflow.md` (ADR section). If the decision does not match (it is
   operational/per-slice, not structural), **say so and do not create it** — the right place
   is probably the decision log (`/dl`).
2. **Read the real template** at `docs/adr/0000-adr-template.md` — the single source of the
   structure.
3. **Compute the next number**: Glob `docs/adr/[0-9][0-9][0-9][0-9]-*.md`, highest NNNN + 1.
4. **Create `docs/adr/NNNN-<kebab-title>.md`** with the template's sections (Status / Context /
   Decision / Consequences / Alternatives Considered). Initial status: **`Proposed`** (the
   owner flips it to `Accepted` on approval).
5. **No architecture theater**: Context describes the real problem that motivated the
   decision; each entry in Alternatives Considered has a **concrete** rejection reason;
   Consequences lists the honest ones — positive AND negative.
6. **Supersedes?** If this ADR revises/replaces another, edit the old one adding the note
   (`Superseded by ADR-NNNN`) — revised decisions get a new ADR, the old one is never deleted.
7. **Update the index** `docs/adr/README.md`: new table row
   (`| [NNNN](NNNN-....md) | Title | theme |`).
8. **Report**: file created, status `Proposed`, pending item = the owner's approval.

## Rules

- Language: **en-US** (project language policy).
- ADRs are few and structural; per-slice autonomous decisions go to `/dl`. Revising an
  inherited rule from `docs/DECISIONS-BASELINE.md` = a new ADR citing the baseline number.
