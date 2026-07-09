# ADR 0023: Configurable File Storage Adapters

## Status

Accepted

## Context

FKMed currently stores profile photos, appointment medical orders and reimbursement attachments as
`bytea` columns in their business tables. This is simple and transactionally strong for the POC,
but the owner now requires local filesystem storage for development, Amazon S3 for production and
the current PostgreSQL behavior retained as a selectable adapter. Files may outlive a configuration
change, and filesystem/S3 operations cannot participate atomically in a PostgreSQL transaction.

## Decision

We will introduce `domain.upload.FileStorage` as the provider-neutral port and implement
PostgreSQL-binary, local-filesystem and Amazon-S3 driven adapters under `infra.storage`. New writes
will use the backend selected by `FKMED_STORAGE_BACKEND`; every persisted reference will be
provider-qualified (`postgres:`, `filesystem:` or `s3:`), so reads and deletes route to the original
provider independently of the current write selection. Development defaults to
`/fkmed/uploads` filesystem storage, automated backend tests default to PostgreSQL, and production
defaults to private encrypted S3. External writes are compensated on transaction rollback and old
objects are deleted only after commit.

## Consequences

- **Positive:** business modules no longer know storage technology; the existing database behavior
  remains available; backend changes do not invalidate prior references; production file volume no
  longer inflates business tables, WAL and database backups.
- **Negative:** metadata and content are no longer covered by one physical transaction for
  filesystem/S3; compensation can leave an orphan when cleanup itself fails; three adapters and AWS
  SDK configuration increase operational and test surface.
- **Operational:** changing the write backend is not a bulk migration. Backup/restore must cover the
  selected object store as well as PostgreSQL.

## Alternatives Considered

- Keep `bytea` only - rejected because it does not satisfy the owner-required filesystem and S3
  deployment strategies.
- Select one adapter at dependency-injection time and store unqualified keys - rejected because a
  later backend switch would make existing objects unreadable or require a flag-day migration.
- Store duplicate content in PostgreSQL and S3 - rejected because it doubles sensitive-data
  retention, creates ambiguous source-of-truth behavior and defeats the storage separation.
- Use browser-to-S3 presigned uploads now - rejected because current magic-byte validation happens
  synchronously in the backend and a safe quarantine/finalization workflow is a separate slice.

## Revision Triggers

- Direct-to-object-store uploads become necessary for materially larger files or measured backend
  transfer pressure.
- Malware quarantine/scanning becomes a production requirement.
- Storage-provider migration or cross-region disaster recovery requires an asynchronous copy and
  reconciliation workflow.

## References

SPEC-0019, SPEC-0006, SPEC-0009, SPEC-0015, SPEC-0017, ADR-0022, DL-0035, Flyway V28.
