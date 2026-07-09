# DL-0035 - Storage reference routing and transaction semantics

- **Phase/slice:** Post-6 / configurable file storage
- **Spec(s):** 0019 (BR3, BR5, BR8, BR11)
- **Related ADR:** ADR-0023
- **Date:** 2026-07-08
- **Status:** ASSUMED
- **Confidence:** High
- **Reversibility:** Moderate

## Gap

The owner selected three adapters and their environment roles, but did not prescribe how references
survive backend changes, how automated tests avoid host filesystem coupling, or how non-transactional
filesystem/S3 operations interact with database rollback.

## Decision

- Persist references as `<provider>:<server-generated-key>`.
- Use the configured backend only for new writes; route reads/deletes by the persisted provider.
- Keep integration tests on the PostgreSQL adapter while the isolated E2E stack uses filesystem.
- Delete a newly written object after transaction rollback.
- Defer deletion of replaced/removed objects until database commit.
- Generate every new key from a fixed server-side namespace plus UUID; never use client filenames.

## Justification

Provider-qualified references allow gradual backend changes without a flag-day data migration.
PostgreSQL keeps deterministic integration tests and proves the retained adapter. Transaction
synchronization closes the most likely consistency gaps without introducing an outbox or background
workflow before measured need.

## Alternatives discarded

- Interpret every key using the current backend - rejected because changing `.env` would strand
  existing content.
- Run all tests against filesystem - rejected because it would leave the PostgreSQL adapter and its
  Flyway migration under-tested.
- Delete old content before commit - rejected because rollback would leave committed metadata
  pointing to content that no longer exists.
- Add an outbox and reconciliation scheduler now - rejected as disproportionate for current file
  sizes and traffic; cleanup failures remain observable and can justify that later.

## Impact

Adds SPEC-0019, ADR-0023, Flyway V28, the storage port/adapters, AWS SDK v2, environment variables,
transaction synchronization tests and metadata-only business entities. Public HTTP contracts and
user journeys remain unchanged.

## How to revert

Keep the port and select `postgres` for all environments. Reintroducing bytes into each business
table would require a new Flyway migration that reads every provider and backfills the old columns.
