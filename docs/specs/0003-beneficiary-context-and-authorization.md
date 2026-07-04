# 0003 - Beneficiary Context and Authorization

**Status:** Approved

## Goal

Every journey in the portal operates on an **active beneficiary** under a strict
family-scope authorization matrix: a titular acts for themselves and their dependents, a
dependent only for themselves — with authorship always recorded, sensitive data masked by
default, an immutable audit trail, and unique business protocols. This spec is the
cross-cutting contract every other module references.

## Scope

- Authorization matrix and its server-side enforcement on every API call.
- Active-beneficiary selector (global component) + accessible-beneficiaries API.
- Immutable audit trail (storage + recording contract used by all modules).
- Masking norms for sensitive data and the "no personal data in URLs" rule.
- Unique business-protocol generation (`<PREFIX>-AAAAMMDD-####`).

## Business Context

The portal serves a family unit: the titular (contract holder) and their dependents.
Accounts are personal (SPEC-0002), but a titular operates dependents' data from their own
account by switching the **active beneficiary**. Health data is sensitive (LGPD): access
outside the family scope must not even reveal that the resource exists, and access to
dependents' sensitive data must be auditable.

## Business Rules

- **BR1** — Authorization matrix: a **titular** MUST be able to read and operate their own
  data and their dependents'; a **dependent** only their own; a **visitor** only public
  access screens. The Financeiro module is titular-only (enforced in SPEC-0013).
- **BR2** — Any attempt to access data of a beneficiary outside the user's family scope
  MUST be denied **without revealing the existence of the resource** (respond as not
  found).
- **BR3** — The server MUST validate the target-beneficiary parameter of **every** request
  against the authenticated user's accessible scope; the client-side active-beneficiary
  context is convenience, never authority.
- **BR4** — Every transactional write MUST record the author (user account) and the target
  beneficiary at confirmation time.
- **BR5** — The selector MUST display avatar, first name and role (e.g. "MARIA ·
  Responsável") and list exactly the beneficiaries accessible to the user; switching the
  active beneficiary MUST reload the current screen's data for the new context.
- **BR6** — The audit trail MUST record, at minimum: authentication events (SPEC-0002),
  registration data changes, term acceptances, viewing of a dependent's sensitive data
  (digital card/CNS, clinical documents), and state transitions of reimbursements,
  appointments and guides — each entry with author, target beneficiary, event type,
  timestamp, IP and device identification.
- **BR7** — Audit entries are **immutable and append-only**; no API or job may update or
  delete them.
- **BR8** — Masking: CPF, CNS and bank data MUST be displayed masked everywhere, except in
  the specific contexts each spec explicitly allows (e.g. CNS in full only on the digital
  card screen and its PDF — SPEC-0007). Personal data MUST NOT appear in URLs (path or
  query).
- **BR9** — Every business transaction defined by the specs (appointment, reimbursement
  request, reimbursement preview) MUST generate a unique protocol in the format
  `<PREFIX>-AAAAMMDD-####` (prefix per module: `AG`, `RE`, `PV`), displayed to the user and
  usable in service channels. Uniqueness MUST hold under concurrent generation.
- **BR10** — Audit entries MUST be retained for **12 months** and then purged by an
  automatic scheduled job; the purge MUST remove only entries older than the retention
  window and MUST itself leave no gap that hides tampering (owner decision, 2026-07-04).

## Input/Output Examples

- `GET /api/context/accessible-beneficiaries` (MARIA) → `200`
  `[{"beneficiaryId":"…","firstName":"MARIA","role":"TITULAR"},{"beneficiaryId":"…","firstName":"PEDRO","role":"DEPENDENT"}]`.
- Same call as PEDRO (dependent) → `200` with only PEDRO.
- `GET /api/guides?beneficiaryId=<someone else's>` (PEDRO) → `404`
  `{"code":"context.beneficiary-not-accessible"}` (error case — existence not revealed).

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/context/accessible-beneficiaries` | Selector data source |

The scope check and audit recording are internal contracts (application services) consumed
by every module — not public endpoints.

## Events

Not applicable (modules publish their own domain events; audit entries are written in the
same transaction as the action they record).

## Persistence Changes

Migration (number at implementation): `audit_event` (id, occurred_at, author_account_id,
target_beneficiary_id nullable, event_type, details JSONB with masked values, ip,
user_agent) — append-only, indexed by target and occurred_at; `protocol_sequence` (prefix,
date, counter) with atomic increment. Audit event types are constants (`*Codes` class),
not an enum (baseline §0019 — technical classification kept as validated codes). A scheduled
job purges `audit_event` rows older than the 12-month retention window (BR10).

## Validation Rules

`beneficiaryId` request parameters MUST reference a beneficiary in the caller's scope
(BR3). Protocol format strictly `^[A-Z]{2}-\d{8}-\d{4}$`.

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Beneficiary outside caller's scope | `context.beneficiary-not-accessible` | 404 |

## Observability Requirements

The audit trail itself (BR6); metric counter of scope-denied attempts (possible probing);
audit writes logged at debug level without personal data.

## Tests Required

- **Domain/unit:** protocol generator (format, date rollover); concurrency property test
  for uniqueness (jqwik or striped-lock integration test).
- **Integration (Testcontainers):** scope enforcement on representative endpoints of each
  consuming module; audit rows written with author/target; immutability (no update path).
- **API contract:** accessible-beneficiaries endpoint.
- **Frontend unit:** selector rendering/switching reload behavior.
- **E2E:** dependent attempting a titular-scoped resource sees the friendly denial.

## Acceptance Criteria

- **AC1** (BR1, BR5) — Given MARIA authenticated, when opening any screen with the
  selector, then she sees herself and PEDRO; given PEDRO authenticated, he sees only
  himself.
- **AC2** (BR2, BR3) — Given PEDRO authenticated, when requesting a resource of MARIA by
  direct URL/id, then the response is the not-found denial and no data leaks (error case).
- **AC3** (BR4) — Given MARIA performs a transactional action with PEDRO as active
  beneficiary, then the record stores MARIA as author and PEDRO as target.
- **AC4** (BR6) — Given MARIA views PEDRO's digital card, then an audit entry exists with
  author, target and timestamp.
- **AC5** (BR9) — Given two simultaneous protocol generations for the same prefix and day,
  then both succeed with distinct sequential numbers.
- **AC6** (BR10) — Given an audit entry older than 12 months and one within the window, when
  the purge job runs, then only the older entry is removed and the recent one remains.

## Open Questions

- ~~**OQ1** — Audit-trail retention period~~ — **answered by the owner (2026-07-04): 12
  months with an automatic purge job** (folded into BR10).

## Out of Scope

A user-facing audit screen; role administration (roles derive from the operator's load);
consent management beyond term acceptances; multi-contract/multi-plan users (POC assumes
one plan per family).
