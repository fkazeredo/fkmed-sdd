# SPEC-0019 - Configurable File Storage Adapters

## Status

Approved

## Goal

Decouple uploaded file content from business tables and allow FKMed to select PostgreSQL binary
storage, a local filesystem, or Amazon S3 through environment configuration without changing
business services or invalidating files written by a previously selected backend.

## Scope

- One file-storage port shared by profile photos, appointment medical orders, reimbursement
  documents, reimbursement previews and pendency documents.
- Three driven adapters:
  - PostgreSQL binary storage;
  - local filesystem storage;
  - Amazon S3 storage.
- Environment-based backend selection for new writes.
- Provider-qualified references so existing files remain readable after the write backend changes.
- Migration of current `bytea` content from business tables to the PostgreSQL adapter.
- Transaction-aware cleanup for external writes and deletes.

## Out of Scope

- Browser-to-S3 presigned upload.
- Malware scanning and quarantine workflow.
- Bulk migration between storage providers.
- Backup/restore automation and retention policy implementation.
- End-user document download endpoints that do not already exist.

## Business and Technical Rules

- **BR1** - Business modules MUST depend only on `domain.upload.FileStorage`; they MUST NOT import
  AWS SDK, filesystem or JDBC adapter types.
- **BR2** - `FKMED_STORAGE_BACKEND` MUST select the destination for new writes and accept exactly
  `postgres`, `filesystem` or `s3`.
- **BR3** - Stored references MUST include their provider. Reads and deletes MUST route by the
  reference provider, not by the currently selected write backend.
- **BR4** - The `dev` profile MUST default to filesystem storage rooted at `/fkmed/uploads`.
  `FKMED_STORAGE_FILESYSTEM_ROOT` MAY override that path.
- **BR5** - Automated backend integration tests MUST default to PostgreSQL storage. E2E MAY use an
  isolated filesystem because its stack is disposable.
- **BR6** - The `prod` profile MUST default to S3 and MUST fail startup unless bucket and region are
  configured. AWS credentials MUST use the AWS SDK default provider chain and MUST NOT be committed.
- **BR7** - Filesystem paths MUST be generated server-side and constrained below the configured
  root. Client filenames MUST never become filesystem paths or S3 object keys.
- **BR8** - A file written during a transaction that rolls back MUST be removed. Replacement or
  removal of an existing file MUST delete the old object only after the database commit.
- **BR9** - S3 objects MUST be private and server-side encrypted. The adapter MUST use SSE-S3 by
  default and SSE-KMS when `FKMED_STORAGE_S3_KMS_KEY_ID` is set.
- **BR10** - Domain metadata MUST retain detected content type, safe display filename, size and
  timestamps. Business tables MUST retain only the opaque storage reference, never provider SDK
  objects.
- **BR11** - Switching the write backend does not migrate existing objects. A future bulk migration
  MUST preserve or atomically replace provider-qualified references.

## Persistence Changes

Flyway V28 creates `file_blob(object_key, content, created_at)` for the PostgreSQL adapter. It
backfills existing profile, appointment, reimbursement and preview bytes with deterministic
PostgreSQL references, adds `storage_reference` to each owning table and removes their binary
columns after the backfill.

## Configuration

| Variable | Meaning |
|---|---|
| `FKMED_STORAGE_BACKEND` | `postgres`, `filesystem` or `s3` |
| `FKMED_STORAGE_FILESYSTEM_ROOT` | Absolute local root, default `/fkmed/uploads` |
| `FKMED_STORAGE_S3_BUCKET` | Private S3 bucket, required in production |
| `AWS_REGION` | S3 region, required in production |
| `FKMED_STORAGE_S3_PREFIX` | Optional object-key prefix, default `fkmed` |
| `FKMED_STORAGE_S3_KMS_KEY_ID` | Optional KMS key id/ARN; blank uses SSE-S3 |
| `FKMED_STORAGE_S3_ENDPOINT` | Optional endpoint for S3-compatible test infrastructure |
| `FKMED_STORAGE_S3_PATH_STYLE` | Optional path-style addressing flag |

## Error Behavior

Adapter failures are infrastructure failures and MUST result in the existing generic HTTP 500
contract without leaking bucket names, filesystem paths, credentials or provider exception details.
Missing content is an operational integrity failure, not a beneficiary-visible "not found" result.

## Observability Requirements

- Log backend and operation outcome without file bytes, client filename or personal identifiers.
- Expose counters for storage writes, reads, deletes and failures, tagged only by backend and
  operation.
- Cleanup failures after commit MUST be logged for reconciliation.

## Tests Required

- Unit tests for provider-reference parsing/routing and transaction completion behavior.
- Filesystem adapter tests for round trip, deletion, atomic replacement and traversal rejection.
- PostgreSQL adapter integration test against Testcontainers.
- S3 adapter unit test asserting bucket/key/encryption and response handling with a mocked SDK.
- Existing profile, appointment and reimbursement integration journeys remain green.
- Configuration tests prove `dev -> filesystem`, tests/default -> PostgreSQL and `prod -> S3`.

## Acceptance Criteria

- **AC1** - Given `FKMED_STORAGE_BACKEND=postgres`, when a photo is uploaded, then its entity stores
  a `postgres:` reference and the bytes exist only in `file_blob`.
- **AC2** - Given filesystem storage, when a file is stored and read, then it exists below the
  configured root and no supplied display filename influences its path.
- **AC3** - Given S3 storage, when a file is stored, then the adapter writes to the configured
  private bucket with server-side encryption.
- **AC4** - Given a `postgres:` reference while filesystem is the active write backend, when it is
  read, then the PostgreSQL adapter serves it.
- **AC5** - Given an external object written inside a transaction, when the transaction rolls back,
  then that object is deleted.
- **AC6** - Given a profile photo replacement, when the database transaction commits, then the new
  reference is retained and the old object is deleted after commit.
- **AC7** - Given the production profile without an S3 bucket or region, then startup fails with an
  actionable configuration error.

## Open Questions

None.
