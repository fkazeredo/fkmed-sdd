---
description: >
  Bootstraps a new project from this template (modular Java/Spring Boot + Angular ERP):
  parameterizes names/package, resets the product artifacts (specs, decision log, changelogs,
  manual, roadmap), preserves the method (templates, architecture, gates, .claude) and
  delivers a green walking skeleton with CI from day one. Manual invocation only —
  destructive and rare.
argument-hint: <product-name> <java-base-package> [domain description]
disable-model-invocation: true
---

# /new-project — bootstrap a new project from the template

All communication is in **pt-BR**. This skill is destructive by nature (it resets artifacts) —
follow the guards to the letter.

## 0. Safety guard (mandatory)

Confirm you are running in the **NEW** repository (a clone/copy of the template), not in the
original: `git remote -v` + directory name. If the remote/directory is the original template
repo, **STOP immediately** and warn the user. Never run the resets on the template.

## 1. Collect the product context (ask, never invent)

Domain, actors, first value journey — the input for the initial spec. Whatever the owner did
not answer becomes an Open Question (invariant 3).

## 2. Follow the official sequence

Read and follow `docs/architecture/workflow.md` §New project creation: initial spec → domains
→ minimal **runnable** skeleton → docs → dev setup → basic tests → CI. **Forbidden** (Rule
Zero): a giant empty architecture, fake bounded contexts, placeholder classes "for later".

## 3. Run the parameterization

Follow the checklist in [parameterization.md](parameterization.md) — the concrete list of
what to **preserve / parameterize / reset**.

Critical point: **rename the Java base package NOW** (`com.example.product` (the template placeholder) → the package from the
argument) — postponing gets expensive (the parent project learned this the hard way). Adjust
everything citing the package: ArchUnit, Spring Modulith, Checkstyle,
`@SpringBootApplication` scan.

## 4. First spec and version

- Write the new product's **SPEC-0001** (walking skeleton) via `/spec`, guiding the first
  value feature.
- The version is born **0.1.0** at the first delivery (DECISIONS-BASELINE §0015); changelogs zeroed
  with the new product's header.

## 5. Prove it runs

- `cd backend && ./mvnw verify` green; frontend `npm run lint && npm test && npm run build`
  green.
- `docker compose up -d` → health `UP` (the `/dev-env` smoke test is the script).
- The template's CI is already copied — adjust image names (GHCR) and confirm the workflows
  reference the new repo.

## 6. Final report (pt-BR)

What was **preserved** from the method, what was **parameterized**, what was **reset**, and
what remains **pending for the owner on GitHub**: branch protection (main/develop), CI
secrets, CODEOWNERS with the new team's real handles.
