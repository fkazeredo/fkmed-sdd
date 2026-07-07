# ADR 0021: Module map revision — `domain.support` (Canais de Atendimento e FAQ)

## Status

Proposed

## Context

SPEC-0014 introduces every contact channel with the operator in one place (BR1/BR2), the antifraud
guidance section reachable by direct anchor (BR3, destination of the Home fraud banner — SPEC-0005
BR9), the Central de Libras service-request registration (BR4), and a searchable FAQ (BR5/BR6).
This closes Phase 5 ("Plano e finanças", specs 0012/0018/0013/0014). None of it fits an existing
bounded context: it is not plan/beneficiary data (`domain.plan`), not finance (`domain.finance`),
not a guide/token (`domain.guides`) — it is operator-managed support content plus one small,
family-scoped write (the Libras request). ADR-0001's "grow the map one spec at a time" policy is
triggered.

## Decision

Add **`domain.support`** as the 14th verified Modulith module, owning `support_channel` (type —
`ChannelTypeCodes` — label, optional sublabel for a type with more than one row, value, optional
hours, display order), `support_antifraud` (a single content row — title/message, DL-0023),
`faq_entry` (category — `FaqCategoryCodes` — question, answer, display order, active) and
`libras_request` (beneficiary, timestamp, `LibrasRequestSituation` — a genuine enum, a state
machine, per DECISIONS-BASELINE §0019). Every read (`GET /api/support/channels`, `/antifraud`,
`/faq`) only requires authentication — content-serving, no scope check. The one write,
`POST /api/support/libras-requests`, scope-checks the target beneficiary through `domain.plan`'s
`BeneficiaryAccess` (family scope, SPEC-0003 BR3) the same way `domain.guides.TokenService`
scope-checks token generation, and always audits the request (`domain.audit`, unlike the
dependent-only rule of `DEPENDENT_TOKEN_GENERATED`). The Libras operating-hours window is a plain
domain constant (`LibrasHours`, DL-0024), not a Spring-config binding, since `domain` code must not
depend on `infra`. FAQ search normalizes case/accents server-side and counts a zero-result search as
a content-gap signal (§Observability); Libras requests are counted the same way.

## Consequences

- **Positive:** a small, mostly read-only module with one well-understood write path; reuses two
  proven cross-module facades (`BeneficiaryAccess`, `AuditRecorder`) instead of inventing new ones;
  closes Phase 5 with the last of its four specs.
- **Negative:** a 14th module raises the ModularityTest/diagram surface further; the antifraud
  content/Libras hours split (DL-0023/DL-0024) means a future owner decision to make Libras hours
  operator-editable at runtime would need revisiting `LibrasHours`' placement.

## Alternatives Considered

- Fold support content into `domain.content` (already owns Home banners/notices) — rejected: a
  different bounded context (contact channels + FAQ + a family-scoped write vs. Home's pure
  read-only banner/notice view), and `domain.content` explicitly "depends on no other business
  module" per its own package-info — adding `BeneficiaryAccess`/`AuditRecorder` dependencies for one
  unrelated write path would blur that boundary.
- Model `ChannelTypeCodes`/`FaqCategoryCodes` as registry tables — rejected (DECISIONS-BASELINE
  §0019): both sets are fixed by the product's own structure (4 channel types, 6 FAQ categories), no
  runtime editing or per-code wired branching, so a `*Codes` constants holder is the correct keep
  criterion, not a registry.

## Revision Triggers

- The owner wants channel numbers, FAQ content or Libras hours editable through an admin UI rather
  than migrations — would introduce a genuine content-management write path.
- A live-chat or ombudsman-protocol feature (both explicitly out of scope now) would add write paths
  and likely an external integration (`domain.support` → `infra` port).

## References

SPEC-0014 · SPEC-0005 (Home fraud banner destination, BR9) · SPEC-0003 (family scope) ·
SPEC-0015/16/17 (reimbursement rules the FAQ teaches, BR6) · ADR-0001 (module map) ·
DECISIONS-BASELINE §0019 (registry vs enum vs code) · DL-0023 · DL-0024 · migration
`V25__support_channels_and_faq.sql` · diagram `docs/architecture-diagrams/modules.puml`.
