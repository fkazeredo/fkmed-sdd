# Observability and Performance

> Read when: adding/changing logs, metrics, tracing, health checks, or doing any
> performance-related work.

## The stack

- **Metrics:** Micrometer → `/actuator/prometheus` (role-gated `ROLE_IT`); Prometheus scrapes
  it natively via OAuth2 `client_credentials` (confidential client `prometheus-scraper`,
  scope `metrics.read` — no manual tokens). Business metrics via an infra event
  listener (`BusinessMetrics`, e.g. `app_platform_job_runs_total{status}`). Standard set:
  `app_outbound_breaker_state{breaker}` (0/1/2, gauge held by a strong reference —
  Micrometer gauges are weak-ref), one Timer per outbound integration
  (`app_<integration>_calls{operation,outcome}`, manual — no AOP),
  `app_identity_login_failures`/`_lockouts` (aggregate, no username tag — PII/cardinality)
  and expiry gauges for any managed credential/certificate (hourly cached, NaN when none —
  never queries on scrape).
- **Logs:** structured JSON on the container console (ECS format by
  default, `LOGGING_STRUCTURED_FORMAT_CONSOLE`), personal data masked; MDC carries
  `correlationId` (every request) **and `username`** (authenticated requests only —
  `UserMdcFilter`, via the `UserContextProvider` port; e-mail/userId deliberately excluded).
  Shipped by **Grafana Alloy** to **Loki** through a parsing pipeline: **`level` is a LABEL**
  (bounded ~5 values — stream selector, alertable) and **`correlationId`/`username` are
  STRUCTURED METADATA** — the cardinality golden rule: an unbounded value must NEVER become a
  label (one stream per value explodes the index). A **derived field** on the Loki datasource
  turns the correlationId inside a log line into a "Related logs" link (all logs of that
  request). **Runtime log levels:** `/actuator/loggers` is exposed, gated `ROLE_IT` (raise a
  logger to DEBUG without redeploy; levels reset on restart). Prod mounts the same
  `loki-config.yml` as dev (parity fix — it used to run the image default).
- **Dashboards:** **Grafana** pre-provisioned — 5 dashboards: *Backend Overview* (landing),
  *Application Health (RED)* (availability, rate, error %, p50/p95/p99, per-status, top-10
  endpoints, breakers, outbound integrations), *JVM, Pool & Cache* (heap/GC/threads/CPU,
  full HikariCP, Caffeine hit-ratio/size), *Business & Jobs* (KPIs, events/h, jobs by
  status, logins×failures×lockouts, credential days-to-expiry) and *Logs* (volume by level,
  live errors/warnings, browse by container, correlation search). No template variables
  on Prometheus dashboards (single app/instance — dead UI); the Logs dashboard DOES use them
  (`$container`, `$correlationId` — there are N containers).
- **Alerts (10, Grafana-managed):** 5xx > 5%, p95 > 2s, Hikari pool > 90%, job FAILED, target
  down + heap > 90%/10m, certificate < 30 days, login-failure spike > 25/15m
  (password spraying — the per-account lockout caps ONE account; the aggregate is the
  signal), breaker OPEN 5m + **error-log spike > 10/5m on the Loki datasource**
  (catches failures with NO HTTP footprint: scheduled jobs, event listeners, best-effort
  mail). All new rules use `noDataState: OK` so dev stays silent. **Delivery: e-mail via SMTP** — contact point `ops-email` + notification
  policy provisioned; Grafana reuses the app's `SPRING_MAIL_*`/`MAIL_FROM` via `GF_SMTP_*` +
  `ALERT_EMAIL_TO` (compose). Without SMTP, alerts are panel-only.
- **Deliberately NOT added (Rule Zero):** p99/GC/cache-ratio/self-monitoring alerts
  (noise — no operator action); **postgres_exporter** (actionable DB signals are app-side —
  Hikari + `db` probe; revisit trigger: a Hikari-saturation incident unexplainable via
  `pg_stat_activity`); **OpenTelemetry** (single-process monolith — correlationId covers it);
  **external-dependency HealthIndicators** (`/actuator/health` is public AND the compose
  healthcheck — an SMTP outage must not restart the app; `management.health.mail.enabled=false`
  guards against Boot auto-registering one when `spring.mail.host` is set).
- **Retention:** dev has a persistent `prometheus-data` volume + explicit 15d; prod 60d + 4GB
  size cap (cardinality-accident guard).
- **Version:** `GET /api/version` (version, commit, build time — from `build-info` +
  `git-commit-id` Maven plugins).
- **Health:** `/actuator/health` distinguishes liveness/readiness; compose healthchecks use it.

Everything ships in `docker-compose.yml` (dev) and `compose.prod.yaml` (prod; Grafana bound
to loopback, password required, no anonymous access).

## Observability rules

Observability is architecture, not optional polish. Logs are structured, contextual and
safe; distinguish application flow, business events, integration, error, audit and security
logs. Logs **MUST** answer: what happened, when, which user/request/job/message, which
business entity, success or failure, duration, and failure class (validation, business,
infrastructure, integration, bug). Never log secrets; mask personal data (LGPD).

Every relevant request, message, job and async flow **SHOULD** carry a correlation ID.

Alerts are actionable. AI/DSS observability: insights carry evidence + provenance; human
decisions on insights are recorded (accepted/rejected), never silently applied.

## Performance

Avoid obviously bad choices from the start: N+1 queries, missing indexes, large object
graphs, unbounded queries, missing pagination, huge payloads, external calls in loops,
synchronous long-running work, missing timeouts, screens loading too much data (the
dashboard KPI endpoint aggregates server-side for exactly this reason).

Heavy optimization is evidence-driven only: logs, metrics, traces, profilers, execution
plans, slow query logs, load tests, production-like volumes. Do not introduce complex
caching, async processing, denormalization or distribution without reason. Code stays
readable unless a measured hotspot justifies complexity.
