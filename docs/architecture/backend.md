# Backend Architecture (Java / Spring Boot)

> Read when: writing or changing any backend code — services, entities, DTOs, mapping,
> validation, error handling, dates, naming, comments.

## Stack (final — v0.51.1)

| Concern | Choice | Version |
|---|---|---|
| Language / runtime | Java (LTS) | 21 |
| Framework | Spring Boot | 4.1.0 |
| Module boundaries | Spring Modulith (23 verified modules, acyclic) | 2.1.0 |
| JSON | Jackson 3 (`tools.jackson`, Boot 4 default) | Boot BOM |
| Persistence | Spring Data JPA + PostgreSQL | Postgres 16 |
| Migrations | Flyway (`spring-boot-starter-flyway` + `flyway-database-postgresql`) | Boot BOM |
| Security | Spring Security + OAuth2 Resource Server + **embedded** Spring Authorization Server (DECISIONS-BASELINE §0018) | Boot BOM |
| E-mail / login page | spring-boot-starter-mail (abstract `EmailSender`) + Thymeleaf (custom login) | Boot BOM |
| Sessions / HA | Spring Session JDBC (DECISIONS-BASELINE §0020) | Boot BOM |
| API docs | springdoc-openapi (webmvc-ui) + committed snapshot with drift gate | 3.0.3 |
| Observability | Micrometer + Prometheus registry, Actuator | Boot BOM |
| Cache | Spring Cache + Caffeine (in-process — DECISIONS-BASELINE §0022) | Boot BOM |
| Search | pg_trgm GIN indexes for indexed ILIKE "contains" | Postgres 16 |
| Boilerplate | Lombok (baseline §0013 — policy below) | Boot BOM |
| Tests | JUnit 5 · Testcontainers 1.21.4 · ArchUnit 1.4.2 · jqwik 1.9.2 · PIT 1.17.3 | — |
| Format / style gates | Spotless (google-java-format) 3.7.0 + Checkstyle 10.21.4 | — |
| Coverage gates | JaCoCo 0.8.12 (INSTRUCTION ≥ 0.80, BRANCH ≥ 0.65) | — |

Build with the wrapper only (`cd backend && ./mvnw verify`); never a system Maven.

## Default style and package layout

Pragmatic modular hexagonal architecture, organized by business domain. Hexagonal is a
principle, not folder theater. Spring, Lombok, Bean Validation and JPA annotations are
acceptable; the project does not pretend Spring does not exist.

Three top-level layers (DECISIONS-BASELINE §0012): `domain` (pure hexagon core), `application` (delivery /
driving adapters), `infra` (driven adapters). The real layout:

```txt
com.example.product
  domain                  <- DOMAIN: pure hexagon core (business only), one package per module
    <one flat package per business module — e.g. orders  customers  billing  registry  identity>
      e.g. orders/
        Order.java  OrderItem.java  OrderService.java
        OrderConfirmed.java (domain event)  OrderNotFoundException.java
        Orders.java                 <- public module facade (port consumed by other modules)
        PaymentGateway.java         <- PORT (interface) for a technical adapter
        OrderResponse.java          <- a Response that maps an @Entity stays inside the module
    error                 <- domain kernel: DomainException, ErrorDetails, RateLimited
    money                 <- domain kernel: Money (BigDecimal scale 2, HALF_UP)
  application             <- DELIVERY (driving adapters): entry mechanisms only, entity-free
    api/       OrderController.java  CustomerController.java ...   (REST controllers)
    api/dto/   OrderRequest.java ...                  (request/response DTOs)
  infra                   <- CENTRALIZED technical layer, by concern (DECISIONS-BASELINE §0010)
      billing/  health/  i18n/  integration/  jobs/  observability/  openapi/  platform/
      security/ (UserContext, UserContextProvider, AuthorizationServerConfig, authz matrix)
      time/  web/ (ApiErrorResponse, GlobalExceptionHandler, HttpErrorMapping, PageResponse)
      — Spring config, framework adapters, and the *impls* of module ports such as
      HttpMunicipalNfseService implements billing.MunicipalNfseService
```

**Dependency rule (baseline §0012, ArchUnit-enforced):** `domain` is the pure core — it **MUST NOT**
depend on `application` (delivery: controllers) or on `infra`. Both `application` and
`infra` may depend on `domain`; `application` **MAY** depend on `infra` (delivery wires domain +
infra). Technical adapters live in `com.example.product.infra.<concern>` and implement a **port defined
in the domain module**, so the domain depends on the port, never on infra. Infra MAY read/write
a module's own persistence to run that module's technical adapter (outbox dispatch, mock
gateway); other business modules still must not touch each other's persistence (Spring Modulith).
The delivery layer is **entity-free**: services return view/response records, never `@Entity`.

**MUST NOT** create `domain/application/ports/adapters/in/out` folder trees unless complexity
truly justifies it. Single Maven project with strong package modularity; multi-module only
for shared libraries, separate deployables, very large codebases or independent ownership.

## Services and use cases

A module service is an Application Service: coordinates flow, transactions, repositories,
domain behavior and results. It **MUST NOT** become a dumping ground for business rules —
domain rules live in entities, value objects, enums with behavior, policies or domain
services. **MUST NOT** create explicit `UseCase` classes by default.

Lombok is the pragmatic default for component boilerplate (DECISIONS-BASELINE §0013): `@RequiredArgsConstructor`
for constructor injection (over `final` fields; no field `@Autowired`) and `@Slf4j` for loggers.
Keep a hand-written constructor only when params carry `@Value`/`@Qualifier` or the constructor
has logic.

## Domain entities and JPA

JPA entities **MAY** be domain entities; no artificial domain/persistence separation by
default. Anemic models are not acceptable: entities **MUST** protect invariants and expose
meaningful methods. Entities **MAY** use Lombok `@Getter` and
`@NoArgsConstructor(access = PROTECTED)` for boilerplate (baseline §0013; the project sets
`lombok.accessors.fluent = true`, so getters stay `title()`), but **NEVER** `@Data` or
`@Setter` — they mutate only through meaningful business methods (both ArchUnit-enforced).
Separate persistence models only for concrete reasons (complex legacy mapping, read models
very different from write models, critical domain isolation).

## DTOs, requests, commands and mapping

Request/response DTOs **MAY** be passed to Application Services when it stays simple.
**MUST NOT** create a `Command` class per request by default — use commands when multiple
delivery mechanisms trigger the same use case, the API shape differs from the use case
input, or parameters multiply. Use records, Lombok and builders to reduce boilerplate.

Mapping: prefer explicit factory methods close to the object (`OrderResponse.from(order)`).
Dedicated mapper classes **SHOULD NOT** be the default; MapStruct **MAY** be used for
repetitive many-field mappings.

## Validation

Validate at every relevant boundary: delivery (controllers, consumers, schedulers),
application (preconditions, existence), domain (invariants, transitions), persistence
(constraints, FKs, indexes), integration (incoming/outgoing data). The domain **MUST NOT**
depend on controller validation to remain valid.

## Errors and i18n

Business errors are explicit, specific exceptions (`OrderCannotBeCancelledException`) that extend
the pure **`DomainException`** (domain kernel `com.example.product.domain.error`) carrying only domain data: a stable `code`
(== i18n key) + optional message args, and — when needed — extra domain data via the kernel
interfaces `ErrorDetails` (e.g. the conflicting item ids) or `RateLimited` (a retry duration).
**Domain exceptions carry NO transport concern** (no `HttpStatus`, no headers, no response DTO) —
baseline §0011. The **presentation layer** (`com.example.product.infra.web`) owns the HTTP mapping: a
`@RestControllerAdvice` `GlobalExceptionHandler` + an `HttpErrorMapping` registry (exception
type → status; build-time test enforces completeness) renders the response and sets `Retry-After`.
Every API **MUST** have a global handler and a predictable error structure:

```json
{ "code": "order.cannot-be-cancelled", "message": "...", "fields": [] }
```

User-facing messages **MUST** be internationalized from the beginning
(`messages.properties` + one file per product locale). Never expose raw enum names as
user-facing labels. The project **MUST** define whether backend, frontend or hybrid resolves
API error messages.

## Repositories

Command repositories are aggregate-oriented and **SHOULD** expose explicit locking methods
(`getRequiredForUpdate(id)`) when concurrency risk exists. Read operations are flexible:
query anything that makes sense (projections, views, SQL). Do not force aggregate purity on
read-only queries. Repository interfaces via Spring Data are natural; do NOT create
interface+`Impl` pairs for internal services — interfaces are for real ports (external
providers, messaging, file storage, notification gateways, AI providers, cache, multiple
implementations).

## Cross-cutting types (no `shared` module — baseline §0012)

There is **no `com.example.product.shared` package**; cross-cutting types live where their dependency
direction allows:

- **Domain kernel** `com.example.product.domain.error` — `DomainException`, `ErrorDetails`, `RateLimited`.
  The domain depends on these, so they must sit in the domain (never in infra).
- **Identity** `com.example.product.infra.security` — `UserContext` + the `UserContextProvider` port and
  its `SecurityContextUserProvider` adapter. Controllers (delivery) inject the port.
- **Web/presentation** `com.example.product.infra.web` — `ApiErrorResponse`, `GlobalExceptionHandler`,
  `HttpErrorMapping`, `PageResponse` (the pagination envelope controllers wrap pages with).

Anything technical the domain does NOT import (web error handler, correlation filter, i18n
`MessageSource` config, mail/JWT/STOMP adapters) is `com.example.product.infra.<concern>` (DECISIONS-BASELINE §0010).
i18n message bundles (`messages*.properties`) are resources; the `MessageSource` config is
`infra.i18n`. Prefer small duplication over a bad shared abstraction.

## Dates and timezones

UTC for technical instants. `Instant`/`OffsetDateTime` for real instants; avoid
`LocalDateTime` when timezone matters; `LocalDate` for calendar dates; `Duration` vs `Period`
by meaning. APIs use ISO-8601. Never rely on server default timezone. Timezone-sensitive
rules **MUST** be tested.

## Code style and naming

Readable, explicit, domain-oriented. Business language first; technical suffixes when they
clarify (`OrderController`, `OrderCreatedEvent`, `EtaPredictionProvider`, `S3FileStorage`,
`OrderAccessPolicy`). Avoid vague names (`Manager`, `Helper`, `Util`, `Handler`, `Data`)
unless context makes them precise. Events are named as business facts that happened. Avoid
`ServiceImpl`. Constructor injection only. Streams only when clear; Optional without abuse.

Value Objects when they protect invariants, carry business meaning or group values — never
mechanically for every primitive. Simple enums for simple statuses; enums **MAY** contain
behavior; explicit state machines only when workflow complexity justifies; invalid
transitions throw specific business exceptions; important transitions audited.

## Enums vs registry (reference data) — DECISIONS-BASELINE §0019

A business enum **MUST NOT** be introduced for reference data. The standing rule:

- **Keep an enum ONLY when** it is a **state machine** (`*Status`/lifecycle whose transitions
  the code enforces), a **technical classification** (`*FailureClass`, circuit-breaker states),
  or a value **fixed by law** (`LegalType`, `LegalBasis`). Document the keep criterion in the
  type's Javadoc.
- **Everything else is registry data**: the persisted value is a `String code` validated
  through the registry's validator port, seeded by a Flyway migration (`code` = a stable
  constant name so the JSON contract never changes), label editable at runtime on the
  reference-data screen, and any wired branching goes through `*Codes` constants (e.g.
  `ChargeKindCodes.PENALTY`) with a safe `default` for unknown codes (pure data, no wired
  effect).

## Documentation comments (owner rule, revised)

Javadoc is **REQUIRED** for code that carries business meaning or a contract:

- public module APIs/facades and Application Service public methods;
- domain entities' business methods, domain services, policies;
- business exceptions, integration ports/ACL interfaces;
- any non-obvious logic: validation, orchestration, concurrency, date/time, security.

Javadoc is **NOT required** for: trivial records/DTOs with self-explanatory fields, simple
getters/accessors, controllers that only delegate, and test code. Equivalent rules apply to
other languages (TSDoc, docstrings, KDoc, XML docs).

Comments **MUST** explain intent, contract, constraints, side effects and exceptions — never
restate the name:

```java
/**
 * Cancels an order when the current status allows cancellation.
 * @throws OrderCannotBeCancelledException when the status does not allow cancellation.
 */
public void cancel(CancellationReason reason, String requestedBy) { ... }
```
