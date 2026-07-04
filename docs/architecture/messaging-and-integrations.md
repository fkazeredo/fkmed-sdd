# Messaging, Integrations, Files, Notifications and AI

> Read when: working with events, queues, jobs/schedulers, idempotency, external APIs,
> uploads/imports/exports, notifications, or AI/LLM/ML components.

## Domain events and messaging

When something meaningful happens in the business, represent it as an explicit event in
business language describing a fact that happened (`OrderConfirmed`, `PriceLocked`,
`SpreadRealized`). In this system all domain events are **in-process** (Spring Modulith
application events) — there is no external broker, by design (Rule Zero); the patterns that
matter here are idempotent listeners, `REQUIRES_NEW` when a read-only flow must persist a
side effect (the 20c lesson), and quarantine + replay at the inbound boundary.
Publishing events that leave the process inside a transaction is risky — use the Outbox
Pattern if external delivery ever arrives.

Contracts are proportional to integration risk: internal events may start simple; once
consumed by multiple modules, external services or long-lived processors, treat as a stable
contract (eventId, eventType, version, occurredAt, correlationId, payload, metadata). Schema
Registry/Avro/Protobuf **MAY** be used when governance justifies.

## Background jobs

Simple jobs: `@Scheduled`. Important jobs **MUST** consider: idempotency, concurrency
control/locking, retry policy, timeout, failure state, execution history, metrics, logs,
alerting, correlation ID, safe restart, partial failure and recovery. Critical async flows
**SHOULD** use messaging/outbox/queue-based processing.

## Idempotency

Only where duplicated execution causes real damage: consumers, outbox/inbox processors,
retries, external integrations, payment/billing, notification dispatch, imports, jobs,
double-submit operations. Use database constraints and state checks before building complex
idempotency infrastructure.

## External integrations and resilience

External systems are unreliable by definition. The internal domain **MUST NOT** be shaped by
external APIs, vendor DTOs, ERP schemas or AI providers — use an Anti-Corruption Layer for
relevant integrations. Vendor DTOs **MUST NOT** leak into domain or application services.

Integration boundaries handle: external DTOs, model translation, provider-specific
errors/status, auth details, timeout, retry, circuit breaker, fallback, payload
sanitization, logging, metrics, tracing, correlation ID, response validation.

- Every relevant external call **MUST** have a timeout.
- Retries are intentional; never blindly retry non-idempotent operations.
- Circuit breakers when a dependency can degrade the application.
- Fallback **MUST NOT** silently produce misleading business results.
- Classify failures: TIMEOUT, UNAVAILABLE, RATE_LIMITED, INVALID_RESPONSE,
  AUTHENTICATION_FAILED, AUTHORIZATION_FAILED, BUSINESS_REJECTED, UNKNOWN_ERROR.

Real reference implementations in this codebase: the outbound NFS-e adapter
(`HttpMunicipalNfseService`: `RestClient` + timeouts + retry of transients + the reusable
`OutboundCircuitBreaker`, selected by config with a simulated default; WireMock
emulator under the `emulators` compose profile); the two inbound webhooks (HMAC signature
over `timestamp + "." + body`, 300s anti-replay window) with quarantine + replay
for rejected payloads; and any polling crawler (queue + circuit breaker + dead-letter).

## Files, uploads, imports and exports

Abstract storage when implementation varies (filesystem, S3, Azure Blob, GCS, FTP, legacy).
Business logic **MUST NOT** depend on storage SDKs. Uploads validate size, type, extension,
content type, filename and authorization — never trust extension alone. Important imports
are not improvised in controllers: validation, preview when useful, row-level errors, async
processing, history, idempotency, audit. Large exports run asynchronously.

## Notifications

Anything leaving the application boundary (email, SMS, push, WhatsApp, webhooks, Slack) is an
external integration. Use abstractions (`EmailSender`, `NotificationProvider`, `SmsGateway`).
Important notifications **SHOULD** be asynchronous; a completed business transaction **SHOULD
NOT** fail because a notification provider is down. Messages follow i18n rules.

## AI / DSS integration

A decision-support module (DSS), if the product has one, is **deterministic by design**:
forecasting and outlier detection via pure, auditable math with hand-calculated test anchors, and
the human always decides (ArchUnit-enforced "advises, never commands"). An LLM port
(`InsightNarrator`) exists as a traceable stub for future narrative generation.

If probabilistic AI ever graduates: never treat model output as inherently correct.
Providers/models isolated behind explicit ports or ACLs.

AI output **MUST** be validated before affecting business state: schema, required fields,
types, enums, ranges, business rules, confidence threshold, consistency with system state,
fallback when invalid. Critical decisions still pass deterministic validation, policy checks
or human review.

AI behavior **MUST** be observable: provider/model and version, prompt/template version,
latency, failure/timeout rate, invalid responses, fallback usage, confidence distribution,
human overrides, accepted vs rejected suggestions, cost/token usage. AI-influenced decisions
**SHOULD** be auditable when business impact is relevant.
