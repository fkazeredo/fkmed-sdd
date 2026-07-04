---
description: >
  Updates the user manual (docs/MANUAL.md, in the product's locale(s)) with the capabilities
  the slice delivered — if the product is multilingual, all locale faces stay in sync
  (content, screens, version, history). Part of the Definition of Done for every slice with
  user-visible changes. Use when closing a slice or when asked to update the manual/user
  documentation. Keywords: manual, user manual, documentação do usuário, MANUAL.md.
argument-hint: "[slice/version] [summary of what changed for the user]"
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# /manual — user manual

The manual is for **users/operators, not developers** — plain language, no unnecessary
technical jargon, written in the product's locale(s) (decided at bootstrap; see CLAUDE.md
§Language policy). All conversation with the owner is in **pt-BR**.

## Steps

1. **Identify what changed for the user** in the slice: the branch diff
   (`git diff develop...HEAD --stat`), the slice's spec and new i18n messages. **If nothing
   user-visible changed** (infra/tooling/CI/internal-docs slice), answer "nada a atualizar no
   manual" and **stop** — Rule Zero.
2. **Required structure** (the existing manual already follows it — keep it):
   - Overview — what the system is and who it is for (short).
   - How to access/use — simple steps; commands only when unavoidable, always explained.
   - Features per delivered phase/slice — in business language: each screen/journey, what it
     does and the step-by-step of the main actions.
   - Glossary of business terms when it helps the reader.
   - Manual version history — what changed in each slice, with the matching version/tag.
3. **Content rules**: describe **only what exists** (nothing speculative — Rule Zero);
   screens and labels cited MUST match the real i18n — confirm via Grep in the frontend
   bundles and in the backend message bundles (`backend/src/main/resources/messages*.properties`);
   **never invent a label**; keep an index once it grows.
4. **Update `docs/MANUAL.md`**; if the product is multilingual, update every locale face
   (e.g. `docs/MANUAL.<locale>.md`) with the SAME content, structure, version number and
   history — **in the same slice; no face may lag**.
5. **If screens changed visually**, regenerate the images following
   [screenshots.md](screenshots.md).
6. **Parity verification** (multilingual products — actually run it):
   - Same set of headings in every face (compare `grep "^#"` of each).
   - Image refs `docs/manual/img/*.png` cited == files existing in the folder, in every face.
   - Same version number in every header.
7. The update goes into the **same PR/commit as the slice**.
