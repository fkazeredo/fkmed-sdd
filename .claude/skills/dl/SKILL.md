---
description: >
  Records an autonomous decision in the decision log (DL-NNNN) in the official format and
  updates INDEX.md, with a highlight when Confidence=Low or Reversibility=Costly. Use
  WHENEVER a gap or Open Question is resolved without the owner present, BEFORE writing the
  code that depends on the decision. Keywords: decisão, DL, decision log, gap, Open Question,
  assumido.
argument-hint: <kebab-case-title> [decision taken]
allowed-tools: Read, Write, Edit, Glob, Grep
---

# /dl — record an autonomous decision

Record the decision BEFORE the code that depends on it. All conversation is in **pt-BR**.

> Owner-rule reminder: asking is the default. This skill only applies when the owner has
> **explicitly authorized** the autonomous run — outside that, STOP and ask him instead of
> recording a DL.

## Steps

1. **Read the canonical format** in `docs/RUN-PHASE.md`, section `## docs/decision-log/` — it
   is the single source of the format (header and sections). Use a recent DL as a caliber
   reference (e.g. Glob `docs/decision-log/DL-*.md`, open the latest).
2. **Compute the next number**: Glob `docs/decision-log/DL-[0-9][0-9][0-9][0-9]-*.md`,
   highest NNNN + 1.
3. **Create `docs/decision-log/DL-NNNN-<kebab-title>.md`** with:
   - **Header**: Phase/slice, Spec(s) (with the affected BRs), related ADR (if any), Date,
     Status=ASSUMED, Confidence (High/Medium/Low), Reversibility (Cheap/Moderate/Costly).
   - **Sections**: Gap / Decision / Justification / Alternatives discarded / Impact /
     How to revert.
4. **The justification cites its source**: the roadmap's recommendations, research done, or —
   when it is merely "the most defensible value" — mark **Confidence=Low**.
5. **Update `docs/decision-log/INDEX.md`**:
   - A row in the general list (numeric order).
   - **If Confidence=Low OR Reversibility=Costly**: ALSO add it to the `## ⚠️ Attention`
     highlight table at the top, filling the "why highlighted" column.
6. **Back-annotate the spec**: move the item from `Open Questions` to `Business Rules`,
   marked `ASSUMED (see DL-NNNN)`.
7. **Report immediately** (house rule — never only at the end): DL number, classification,
   and an **explicit alert** if it is Confidence=Low or Reversibility=Costly — the owner
   must see it.

## Rules

- The log is **append-only**: a revised decision = a new DL referencing the old one; never
  edit the original decision beyond the revision note.
- DL content language: **en-US** (project language policy).
