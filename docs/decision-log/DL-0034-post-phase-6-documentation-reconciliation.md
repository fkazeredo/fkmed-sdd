# DL-0034 - Post-Phase 6 documentation reconciliation

- **Phase/slice:** Hardening / docs reconciliation after Phase 6
- **Spec(s):** Docs and governance only
- **Related ADR:** ADR-0001..ADR-0022
- **Date:** 2026-07-08
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

After Phase 6 was merged, several living docs still described the pre-merge or inherited template
state: README still presented Reembolso as a future phase, ROADMAP-STATUS still said PR #28 was open,
architecture pages contained `com.example.product` and parent-project examples, and implemented ADRs
were still listed as `Proposed`.

## Decision

Reconcile documentation to the current repository state:

- mark implemented project ADRs ADR-0001 through ADR-0022 as `Accepted`;
- update README, changelog/status references and SECURITY.md to include Phase 6 and operator-sim
  credentials accurately;
- rewrite architecture docs that contained inherited project/template claims so they describe the
  FKMed POC as implemented, with future production hardening explicitly labeled as future work;
- normalize DL-0030 and DL-0031 to the decision-log format required by RUN-PHASE.

## Justification

The repository uses docs as source of truth for agents and future slices. Leaving stale or inherited
claims in authority docs creates a higher risk than a broad docs-only cleanup, especially now that
the owner explicitly asked to correct every wrong documentation point found.

## Alternatives discarded

- Leave ADR statuses as `Proposed` until a separate governance ceremony - rejected because the code
  already depends on these decisions and the status is misleading.
- Only fix README/ROADMAP-STATUS - rejected because architecture docs are loaded by agents before
  implementation work and can steer future code incorrectly.
- Edit historical final reports - rejected except for adding reconciliation elsewhere; final reports
  are historical evidence and should not be rewritten as living state.

## Impact

Docs-only governance updates plus the decision-log index. No API, schema or user-facing behavior
changes depend on this documentation reconciliation.

## How to revert

Revert the documentation updates, or supersede this DL with a new governance convention if the owner
wants a different ADR lifecycle model.
