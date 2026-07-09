# Observability and Performance

> Read when: adding/changing logs, metrics, tracing, health checks, correlation IDs or
> performance-related behavior.

## Current Stack

- **Metrics:** Micrometer + Prometheus registry. `/actuator/health`, `/actuator/info` and
  `/actuator/prometheus` are exposed at application level; production exposure is controlled by the
  nginx proxy/network boundary, not by an application `ROLE_IT` gate.
- **Logs:** application logs are written to the container console. Access logs include method, path,
  status, duration and a request correlation ID.
- **Correlation:** `AccessLogFilter` accepts a safe inbound `X-Correlation-Id` or generates a UUID,
  stores it in MDC as `correlationId`, echoes it in the response header and removes it after the
  request.
- **Security/privacy logs:** authentication event logs mask e-mail hints; logs must not include
  passwords, tokens, raw CPF/CNS, raw bank data or request bodies.
- **Health:** `/actuator/health` distinguishes liveness/readiness and is used by compose
  healthchecks.
- **Version:** `/api/system/version` is backed by build/git metadata.
- **Dashboards:** dev/prod compose files include Grafana/Prometheus support; dashboard inventory and
  alert rules should be verified against the committed provisioning before being described as
  production guarantees.

Not currently implemented: role-gated Prometheus scraping, `/actuator/loggers` exposure,
`UserMdcFilter`, OpenTelemetry tracing and a complete production alert catalog. Treat those as future
hardening unless a later slice implements them.

## Logging Rules

Logs must answer what happened, where, outcome and duration without leaking personal data.

Required for meaningful request/business logs:

- stable event/action name;
- correlation ID;
- business identifier only when it is non-sensitive or masked;
- outcome/failure class;
- duration where useful;
- no raw payloads containing personal or clinical data.

Use bounded labels/tags for metrics. Unbounded values such as e-mail, user id, beneficiary id,
protocol, correlation id and free-text errors must not become Prometheus labels or Loki labels.

## Correlation IDs

Every request should have a correlation ID. When frontend/manual QA sends `X-Correlation-Id`, the
backend may reuse it if it is short and safe (`A-Z`, `a-z`, digits, `.`, `_`, `:`, `-`). Unsafe values
are ignored and replaced by a generated UUID.

Future async/event flows should copy the same correlation ID when it helps connect request, event,
notification and job logs.

## Metrics

Use business metrics only when they drive a real operational or product question. Good examples in
this app:

- login failures/lockouts as aggregate counters;
- notification dispatch outcomes;
- reimbursement submissions and upload rejections by reason;
- file-storage operations and failures tagged only by backend/operation/outcome;
- finance validation outcomes;
- support FAQ empty searches and Libras requests.

Never tag metrics with personal data.

## Performance

Avoid obvious performance traps from the start: N+1 queries, missing indexes, unbounded lists,
large payloads, synchronous long-running work, external calls in loops and missing timeouts.

Optimize only with evidence from logs, metrics, profiles, execution plans or production-like
volumes. Do not introduce caching, async processing or denormalization just to look enterprise.

The frontend initial bundle currently exceeds the warning budget slightly after Phase 6. Treat
additional growth as debt and prefer lazy loading/dependency trimming over budget inflation.

## Future Production Hardening

Before a real production launch, add or confirm:

- authenticated or network-isolated scraping with a clear threat model;
- structured JSON log format and log shipping pipeline;
- an alert catalog with actionable thresholds and owners;
- runbooks for high error rate, latency, DB outage, mail outage, E2E failures and upload failures;
- privacy review of all logs and dashboards.
