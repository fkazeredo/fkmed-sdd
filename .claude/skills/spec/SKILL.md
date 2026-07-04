---
description: >
  Creates a new spec in docs/specs from the official template, with the next sequential
  number and the index updated. Use when the user asks to create/draft a specification,
  specify a feature, or when a new feature has no spec (CLAUDE.md invariant 4 — spec-driven
  development). Keywords: spec, especificação, specification, new feature.
argument-hint: <kebab-case-title> [goal summary]
allowed-tools: Read, Write, Edit, Glob, Grep
---

# /spec — create a specification

Create a new spec following the project's method. All conversation with the owner is in
**pt-BR**.

## Steps

1. **Read the real template** at `docs/specs/0000-specs-template.md`. It is the single source
   of the structure — never reproduce the sections from memory.
2. **Compute the next number**: Glob `docs/specs/[0-9][0-9][0-9][0-9]-*.md`, take the highest
   NNNN and add 1 (ignore the template's `0000`).
3. **Create `docs/specs/NNNN-<kebab-title>.md`** with ALL the template's sections, in the
   exact order. The template's own rule: a section that does not apply gets
   `Not applicable.` — **never delete a section**.
4. **Fill in** Goal/Scope/Business Context with what the user provided in `$ARGUMENTS`.
   **Never invent a business rule** (CLAUDE.md invariant 3): anything the user did not say
   that affects behavior, contracts, data or security goes into **Open Questions** — not into
   Business Rules.
5. **Business Rules** in direct, testable language (the template's MUST/`409` example style).
   Each rule numbered (BR1, BR2, …).
6. Initial status: **`Draft`**. Spec language: **en-US** (project language policy — specs
   are never translated).
7. **Update the index** `docs/specs/README.md`: new table row
   (`| [NNNN](NNNN-....md) | Title | module/area |`), in numeric order.
8. **Report to the user**: path of the created file, the Open Questions left pending (they
   are his to answer) and the next step — once the spec is approved, start implementation
   with `/slice`.

## Rules

- A spec is a **living artifact**: if a spec covering the topic already exists, update it
  instead of creating another (check the index with Grep before creating).
- Do not write an implementation plan here — a spec is a contract, not a plan (`/slice`
  handles the plan).
