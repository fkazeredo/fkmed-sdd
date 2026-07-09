# Persistence, Data and Caching

> Read when: touching the database, migrations, transactions, locking, deletion, reports,
> search/filters, or caching.

## Database and migrations

The database is **PostgreSQL 16** (Docker locally; Testcontainers in the suite). The schema
is owned end-to-end by **Flyway** (versioned migrations `V1`…`Vn`, including the
Authorization Server/Spring Session state, the reference-data registry seeds and any search
indexes); never edit an applied migration. Jobs claim work with **Postgres advisory locks**,
which keeps them HA-safe across instances.

Database design is architecture. Every schema change is a versioned SQL migration. Never
rely on `ddl-auto=update` in production. The database enforces
integrity: PKs, FKs, unique/not-null/check constraints, indexes, isolation, locks, views.
Slow queries and missing indexes are architecture problems.

Data migrations/backfills: a plain Flyway migration is enough for simple changes. When a
migration affects production data, large tables, critical workflows or compatibility, the
spec/ADR defines the strategy (backfill, expand/contract, background migration, window).

Seeds: essential system data **MAY** go through Flyway; local dev fake data **MUST NOT** mix
with production migrations; tests create their own data via builders/factories/fixtures.

## File content

Business tables store upload metadata and an opaque provider-qualified `storage_reference`, not
binary content. `domain.upload.FileStorage` routes references to the PostgreSQL `file_blob`,
filesystem or S3 adapter (SPEC-0019/ADR-0023). The configured backend controls only new writes;
changing it is not a bulk migration.

PostgreSQL writes participate in the current transaction. Filesystem/S3 writes are compensated on
rollback, while deletion of replaced content runs only after commit. A cleanup failure can leave an
orphan but must never leave committed metadata pointing to content deleted before commit.

## Transactions and consistency

`@Transactional` at the Application Service method representing the use case. Never pretend
a message publish or external call is atomic with a DB commit. Eventual consistency only
with safeguards: idempotency keys, retries, DLQs, outbox/inbox, locking, versioning,
compensating actions, status machines, reconciliation jobs, audit trail, correlation IDs,
metrics, explicit failure states.

## Concurrency and locking

Optimistic locking (`@Version`) is the default for mutable entities at risk. Pessimistic
locking when risk justifies: financial operations, stock, critical transitions, outbox
processing, job claiming. Concurrency conflicts are translated into clear API/domain errors,
never raw database exceptions.

## Deletion

Strategy by business relevance: hard delete for disposable data; soft delete to preserve
records out of normal usage; lifecycle status when deletion has business meaning. Important
deletion-like actions **SHOULD** be audited.

## Reports and heavy reads

Never force every read through domain aggregates. Use query services, projections, DTOs,
SQL, views, materialized views, read models or denormalized tables as appropriate. Large
reports run asynchronously with status, file generation, download link, logs and history.

## Search and filters

Relational database first. Search engines (Elasticsearch/OpenSearch/Meilisearch) only when
requirements justify operational cost. Search endpoints define: filters, searchable/sortable
fields, pagination model, default sort, max page size, empty-result behavior, case
sensitivity, partial match, date/timezone behavior, errors.

## Caching

Cache only when it solves a real problem. Every cache defines: key, value, expiration,
invalidation, expected consistency, max staleness, scope (local/distributed/user/tenant),
metrics, failure behavior, memory impact, serialization. Track hit/miss rate, evictions,
size, latency, errors. Stale data is an architectural trade-off, not an accident.

The cache (DECISIONS-BASELINE §0022): **Caffeine in-process**, one `CacheManager`
(`infra.platform.CacheConfig`, `recordStats()` → Micrometer). Typical caches: the hot
reference-data validation (the registry's `isValid`, called on every validated write) with
eviction on any registry write; the small role catalogue. Local per
instance, short TTL — the small cross-instance staleness for reference data is accepted, not a
distributed cache (Rule Zero). Cache names are string literals in the domain services (the
domain must not import `infra`'s `CacheConfig`).
