# ADR 0003: Single-tenant per build — drop the multi-tenant seam (revises baseline §0003)

## Status

Accepted

## Context

DECISIONS-BASELINE §0003 ("Single-tenant, multi-tenant-ready") is an inherited rule: run
single-tenant, but every business table carries a `tenant_id` (NOT NULL, indexed, fixed
default), queries are tenant-filtered and cache keys tenant-prefixed, so a future move to
multi-tenant needs no business-table migration. The walking-skeleton migration (V1)
implemented that seam (`tenant_id` columns + indexes on `plan` and `beneficiary`).

FKMed's product reality does not match that assumption. The owner confirmed the product is
deployed **one medical provider per build/deployment**: there is no multi-tenant concept
now and none is planned on the roadmap. Under the project authority order (owner request >
baseline), carrying an unused `tenant_id` on every table — never mapped in entities, never
filtered in queries — is a silent divergence from a pre-accepted rule and dead weight that
Rule Zero rejects (a seam for a consumer that does not exist). Revising an inherited rule
requires a new ADR citing the baseline number, which this ADR is.

## Decision

We will run FKMed as a **single-tenant application, one provider per build**, and **drop
the `tenant_id` multi-tenant seam of DECISIONS-BASELINE §0003** for this project. Business
tables carry **no** `tenant_id` column; there is no tenant filter or tenant-prefixed cache
key. The `tenant_id` columns and their indexes are removed from the V1 baseline migration
(edited in place, as V1 is not yet applied to any real environment). Should multi-tenancy
ever become a product requirement, it will be reintroduced deliberately as an architectural
boundary via a new ADR superseding this one.

## Consequences

- **Positive:** simpler schema and code; no unused, unenforced column masquerading as a
  security boundary; the model matches the actual deployment topology (Rule Zero).
- **Negative / risk:** if multi-tenancy is ever needed, it will require a real migration
  adding the discriminator to every business table plus tenant-propagation across queries,
  cache, audit, events and jobs — exactly the future-proofing §0003 was meant to avoid.
  That cost is accepted given no multi-tenant requirement exists or is foreseen.

## Alternatives Considered

- **Keep `tenant_id` unmapped (as delivered)** — rejected: a NOT NULL column that no entity
  maps and no query filters is a silent, unenforced deviation from §0003 and pure dead
  weight; it reads as a security boundary that isn't one.
- **Keep `tenant_id` and fully honor §0003** (map it in entities, filter every query,
  prefix cache keys) — rejected: builds and maintains a multi-tenant machinery for a
  product that is single-tenant per build, with ongoing cost and no consumer (Rule Zero).

## Revision Triggers

- A product requirement to serve more than one medical provider from a single deployment.
- Any data-isolation requirement between distinct providers within one running instance.

## References

- DECISIONS-BASELINE §0003 (the inherited rule this ADR revises).
- `backend/src/main/resources/db/migration/V1__baseline_plan_and_beneficiary.sql`.
- `docs/architecture/security.md` §"single-tenant with extension seams".
- Owner decision, 2026-07-04 (FKMed is one provider per build).
