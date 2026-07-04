---
description: >
  Performs the lockstep version bump (DECISIONS-BASELINE §0015): backend/pom.xml + the
  hardcoded version in OpenApiConfig + OpenAPI snapshot regeneration + a changelog entry (in
  every product locale, if multilingual). Decides MINOR/PATCH from the content; docs-only
  slices do NOT bump. NEVER creates a tag (human action, baseline §0023). Use when closing a
  slice with code or when asked for a bump/release. Keywords: release, versão, bump, SemVer,
  changelog.
argument-hint: "[minor|patch] [release summary]"
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# /release — lockstep version bump

All conversation is in **pt-BR**. Announce what you are about to do before each block.

## Steps

1. **Read the authority**: `docs/DECISIONS-BASELINE.md` §0015 (or the project ADR that
   revised it) decides the digit — MINOR = new backwards-compatible capability, PATCH = fix
   only; breaking changes are highlighted in the release note while the version is `0.y`.
2. **Entry gate**: if the slice is **docs-only** (no code, migration or test touched — check
   with `git diff --stat`), do **NOT** bump — say so and stop (established precedent for
   docs-only slices).
3. **Source of truth**: `backend/pom.xml` `<version>`. Read the current version and compute
   the next one.
4. **Edit in lockstep** (all three places — never just one):
   a. `backend/pom.xml` → `<version>`.
   b. `OpenApiConfig.java` → the hardcoded `.version("X.Y.Z")` (and the version string in the
      description, if present). **Locate it via Glob**
      `backend/src/main/java/**/OpenApiConfig.java` — never by a fixed package path (the base
      package changes in child projects).
   c. Snapshot: `cd backend && ./mvnw verify -Dopenapi.snapshot.write=true` — regenerates
      `docs/api/openapi.json`; the build's drift gate validates the sync.
5. **Changelog(s)**: a new entry at the TOP of `docs/release-notes/CHANGELOG.md`, in the
   file's existing format (`# Release X.Y.Z — … · title` + date/tag line + highlights +
   technical section). Multilingual product ⇒ every locale face gets the entry in the same
   slice — never only one.
6. **Anti-desync verification** (a real historical bug in the parent project): Grep for the
   OLD and the NEW version across the whole repository. Every live occurrence of the old one
   (pom, OpenApiConfig, MANUAL headers when the slice is user-facing) must be fixed or have
   an explicit justification (e.g. historical changelog entries are legitimate).
7. **NEVER** run `git tag` or `gh release create` — the tag is human, cut from `main` via a
   release PR (`develop → main`); `settings.json` enforces this. Just remind the owner in the
   report.
8. **Report**: previous → new version, files touched, `verify` result, and the human-tag
   reminder.
