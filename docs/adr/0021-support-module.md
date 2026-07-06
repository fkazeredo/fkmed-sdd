# ADR 0021: Module map revision — `domain.support` (service channels, antifraud, FAQ, Libras — SPEC-0014)

## Status

Proposed

## Context

SPEC-0014 introduces the Canais de Atendimento screen: operator-managed contact channels (BR1/BR2),
an antifraud section anchored from the Home fraud banner (BR3, closing SPEC-0005's deferred AC6),
a Central de Libras service-request flow (BR4) and a searchable FAQ (BR5/BR6). None of this belongs
to an existing bounded context: it is not Home content (`domain.content` owns banners/notices, not
the destination screen they link to), not `domain.network` (provider search), and not a guide or
token. ADR-0001's "grow the map one spec at a time" policy fires again, as it did for
`domain.guides` (ADR-0018).

## Decision

Add **`domain.support`** as the 13th verified Modulith module, owning three tables: `support_channel`
(type `CENTRAL|WHATSAPP|OUVIDORIA|ANS`, label, value, hours, display_order — BR1/BR2, read-only
registry seeded by migration), `faq_entry` (category **registry code** validated by a `*Codes`
constants holder per DECISIONS-BASELINE §0019 — not an enum, since the six categories are an
operator-editable content vocabulary in spirit even though seeded by migration in this phase —
question ≤ 200, answer, display_order, active) and `libras_request` (beneficiary_id, requested_at,
situation). `ChannelType` and `LibrasSituation` (`REGISTERED|ATTENDED`) are kept as enums: both are
fixed, technical/state-machine classifications the code branches on (rendering per channel type;
the Libras request lifecycle), not a growing business vocabulary — the keep-criterion is recorded in
each enum's Javadoc, mirroring `domain.content.NoticeSeverity`. The antifraud copy (title/message/best
practices) is fixed seeded content with no dedicated table (Rule Zero) — served as a static view from
the module's facade, following the same "read model over migration-seeded content" shape as
`domain.content.HomeContent`. The Libras request write path reuses the `domain.guides.TokenService`
shape: `domain.plan.BeneficiaryAccess.requireAccessible` for the SPEC-0003 scope check, an
`AuditRecorder` entry for the registration (new `AuditEventTypes.LIBRAS_REQUEST_REGISTERED` code) and
a `MeterRegistry` counter, plus a second counter for zero-result FAQ searches (content-gap signal per
SPEC-0014 §Observability). Public API: `GET /api/support/channels`, `GET /api/support/antifraud`,
`GET /api/support/faq` (category/q filters), `POST /api/support/libras-requests`. No new business
error codes — standard auth/scope errors only (content-serving module, like `domain.content`).

## Consequences

- **Positive:** a self-contained support/FAQ context; the FAQ registry-code choice keeps the six
  categories swappable without an enum migration if the operator ever wants a seventh; reuses three
  already-proven patterns (`domain.content`'s content-serving facade, `domain.guides.TokenService`'s
  scope-check + audit + metrics shape, `domain.network`'s case/accent-insensitive Java-side filter)
  instead of inventing a fourth.
- **Negative:** a 13th module raises the ModularityTest/diagram surface again; the FAQ's
  case/accent-insensitive search is filtered in Java (small seed, ~12+ rows) rather than at the
  database layer — revisit with a `pg_trgm`/`unaccent` index if the FAQ catalog grows enough to
  matter (same tradeoff `domain.network`'s `NormalizedText` already accepted and documented).

## Alternatives Considered

- **Fold into `domain.content`** — rejected: `domain.content` is scoped to the Home read model
  (banners/notices); channels/FAQ/Libras are a different screen with their own write path (the
  Libras request), which would force an unrelated write surface onto a module documented as
  read-only content.
- **Model FAQ category as an enum** — rejected (DECISIONS-BASELINE §0019): six categories the
  operator can reasonably want to extend is exactly the "reference data is registry data" case, not
  a state machine or a technical classification.
- **A dedicated `libras_events` audit table** — rejected (Rule Zero): the existing
  `domain.audit.AuditRecorder`/`AuditEventTypes` mechanism already carries this without a new
  structure, the same call the guides module makes for the BR12 dependent-token audit.

## Revision Triggers

- The FAQ catalog outgrowing the in-Java case/accent-insensitive filter (see Consequences) would
  warrant a database-level search index.
- A future operator-side "attend" action on `LibrasRequest` (`ATTENDED`) would add a write surface
  through the operator-sim seam (ADR-0017), the way SPEC-0018 later extended `domain.guides`.

## References

SPEC-0014 (Service Channels and FAQ) · SPEC-0005 BR9/AC6 (Home fraud banner → antifraud anchor) ·
SPEC-0003 (scope check + audit) · ADR-0001 (module map) · ADR-0006 (`domain.content` precedent) ·
ADR-0018 (`domain.guides` — scope-check + audit + metrics pattern reused here) ·
DECISIONS-BASELINE §0019 (registry data vs enum) · migration `V25__support.sql` · diagram
`docs/architecture-diagrams/modules.puml`.
