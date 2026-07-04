# Inherited Architecture Decisions — Pre-Accepted Rules (Baseline)

These rules were **decided, applied and battle-tested** in the parent project this foundation
was distilled from. A project born from this boilerplate starts with them **already in force**
— no re-deciding, no ceremony. Each rule keeps its original ADR number as provenance.

**How to evolve a rule:** write a NEW ADR in your project (starting at your own `ADR-0001`)
that explicitly cites the baseline number it revises (e.g. *"supersedes baseline-0022"*), with
the concrete reason. Never edit this file to change a rule — it is the inherited record.
Product-specific decisions of the parent project were deliberately left out.

## Structure & modularity

**§0001 — Modular monolith with Spring Modulith.** One Spring Boot app, one PostgreSQL.
Business modules with boundaries enforced by Spring Modulith `verify()` + ArchUnit (both break
the build). Cross-module sync calls go through public module APIs only; async via in-process
domain events. Microservices only with a concrete, documented reason.

**§0010/0012 — Three-layer packages: `domain` / `application` / `infra`.** `domain` = pure
hexagon core, one package per module plus a tiny `domain.error` kernel; holds the
`@ApplicationModule` markers. `application` = delivery only (REST api + DTOs). `infra` =
driven adapters and technical config, centralized by concern (`infra.security`, `infra.email`,
`infra.integration`, `infra.time`, `infra.i18n`, `infra.observability`…), each implementing a
**module-owned port**. ArchUnit-enforced: `domain` depends on nothing above it;
`application` → `infra` allowed; `infra` never depends on `application`. No `shared` module.

**§0016 — Flat packages + `@ModuleInternal`.** No `internal/` subpackages. Types that must not
be used across modules are annotated `@ModuleInternal`, enforced by an ArchUnit rule (exempts
same-module and `infra..`). Every new ArchUnit rule ships with a "teeth test" proving it fails
on a planted violation.

**§0009 — Events at the edge of the transaction.** Event publishers live in the module that
owns the fact; external publication only via `@TransactionalEventListener(AFTER_COMMIT)`. No
catch-all "realtime"/"events" module.

## Domain & data

**§0019 — Reference data is registry data, not enums.** A single registry table
(`type, code, label, active, sort_order`, unique on `(type,code)`). Business modules store the
`code` as a plain `String` (a value — never a cross-context FK) and validate it through the
registry's public validator port. Branch logic lives in `*Codes` constant classes with a safe
fallback. Writes are role-protected; values are seeded by migration; DELETE is
soft-deactivation. Business enums only for state machines (`*Status`), technical taxonomies,
or values fixed by law — with the keep-criterion documented in the Javadoc.

**§0011 — Domain exceptions free of transport.** `DomainException` carries only a stable
`code` (== i18n message key) + args. HTTP mapping lives in the presentation layer via a
mapping registry + global exception handler, with a **completeness test** that fails if any
exception subclass lacks a mapping (default 422). No raw persistence exceptions leak to
clients.

**§0003 — Single-tenant, multi-tenant-ready.** Run single-tenant, but every business table
carries `tenant_id` (NOT NULL, indexed, fixed default value), queries are tenant-filtered and
cache keys tenant-prefixed — so going multi-tenant later needs no business-table migration.

**§0013 — Lombok for boilerplate only.** `@Slf4j` + `@RequiredArgsConstructor` on components;
`@Getter` + `@NoArgsConstructor(PROTECTED)` on entities; **never `@Data`/`@Setter` on JPA
entities** (ArchUnit-enforced). `lombok.config`: `stopBubbling`, fluent accessors off for
entities, dependency `optional`/excluded from the jar.

## Integration & communication

**§0006 — External integrations are mock-first behind ports.** Define the port; ship a
simulator adapter that behaves like the real thing (persists a job, delivers an
**HMAC-signed webhook** after a configurable delay, idempotent on `(externalId, eventType)`).
Swapping to the real provider = a new adapter, nothing else moves.

**§0007 — E-mail in layers, best-effort.** One `EmailSender` port, one SMTP implementation,
environment-config only. Sending is async via an outbox table + scheduled worker with retry/
backoff and a `FAILED_PERMANENT` dead-letter. The system works fully without SMTP configured
(clear "not configured" notice). Message content via i18n `MessageSource`.

## Security

**§0018 — Self-hosted OIDC (embedded Spring Authorization Server).** The app is its own IdP:
three ordered `SecurityFilterChain`s (AS endpoints → resource server `/api/**` → form login).
SPA is a public client with PKCE (S256), **no refresh token in the browser** — silent refresh
via SSO session. Local user store with BCrypt. Roles injected into the token by a customizer.
No external IdP unless a real operational reason appears (that is a new ADR).

**§0005 — Session/token hygiene (principles).** Short-lived access tokens; any long-lived
credential is server-side, single-use rotating, `httpOnly+Secure+SameSite`; passwords BCrypt;
login rate-limited and lockout-protected with **generic** error messages (no user
enumeration).

**§0023 — Repository governance & secret scanning.** `main` and `develop` protected: PR-only,
≥1 review, CODEOWNERS, required status checks, linear history, no force-push, admins included.
Agents may push feature branches and open PRs; they never merge/tag/release
(`.claude/settings.json` enforces locally; branch protection enforces on the server). Gitleaks
runs as a blocking CI check + pre-commit hook; `.gitleaks.toml` allowlists ONLY enumerated
dev-defaults; a startup validator refuses to boot in prod with any dev default.

## Operations

**§0002/0020 — Single instance by default, multi-instance ready.** One container per
environment is the operational default (simple schedulers, in-memory rate-limit). The app is
kept stateless-ready: signing keys injectable via env (persisted, not ephemeral, in prod),
auth/session state in JDBC, so scaling out is a config change. Revision triggers: sustained
p99 latency, CPU saturation, throughput ceiling — measured, not guessed.

**§0022 — In-process caching with Caffeine.** One `CaffeineCacheManager` in infra config;
caches are named string literals (domain never imports infra). Defaults: `maximumSize=10_000`,
`expireAfterWrite=10min`, `recordStats()` → Micrometer. Evict `allEntries` on writes to the
cached aggregate. No Redis/distributed cache until 2+ instances actually diverge (revision
trigger).

**§0021 — Backup & DR.** Daily `pg_dump -Fc` + a tar of the document vault at the same
logical instant; retention **30 daily / 12 monthly / 7 yearly** (offsite for the last two);
**RPO 24h / RTO 4h**; quarterly restore rehearsal. Secrets are never inside backups.

## Delivery

**§0015 — SemVer with a single source of truth.** `backend/pom.xml <version>` is THE version;
git tag (no `v` prefix) mirrors it. `0.y.z` = initial development; one MINOR per delivered
phase/slice-group, PATCH for fixes; docs-only changes never bump. Conventional Commits
(`feat`/`fix`/`!`) drive the digit. Tags are cut by a human from `main` via a release PR
(develop → main). `1.0.0` is an owner decision.

**§0017 — Stack baseline.** Java 21 (LTS) + Spring Boot 4.1.x + Spring Modulith 2.1.x +
springdoc 3.x. Upgrades happen via a disposable spike branch with `./mvnw verify` as the
judge, never in place. (Full stack manifest: `docs/BOOTSTRAP.md`.)

**§0008 — Frontend stack.** Angular (standalone components, signals, zoneless — no NgModules,
no NgRx), PrimeNG for data-heavy UI, Tailwind for layout, runtime i18n via ngx-translate,
dev-server proxy for `/api`. OIDC via angular-oauth2-oidc (Code + PKCE).

---

*Left out on purpose (product-specific to the parent project): its locking strategy for the
reservation race (write your own when a hot row appears — the layered pattern
UNIQUE-constraint + scoped pessimistic lock + `@Version` is a good starting point) and its
initial module list (yours comes from your own domain, via the walking-skeleton spec).*
