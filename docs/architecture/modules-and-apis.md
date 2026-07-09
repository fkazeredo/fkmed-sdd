# Modules, Boundaries, APIs and Repositories Layout

> Read when: defining module boundaries, calling across modules, designing/changing endpoints or
> JSON contracts, considering microservices, BFFs or repository split.

## Package Layers

The FKMed backend is a modular monolith under `com.fkmed`.

| Layer | Package | Contents |
|---|---|---|
| Domain | `com.fkmed.domain.<module>` | Business services, entities, repositories, domain events, value/view records, business exceptions and public module facades. |
| Delivery | `com.fkmed.application` | Entry mechanisms only: REST controllers in `api`, transport DTOs in `api.dto`, and dev-only operator-simulation endpoints in `sim`. |
| Infra | `com.fkmed.infra.<concern>` | Security, identity adapters, web/error handling, observability, e-mail dispatch, i18n, time, health, platform checks and OpenAPI wiring. |

`domain` must not depend on `application` or `infra`. `application` and `infra` may depend on
`domain`; `application` may depend on `infra` for security/user context and delivery adapters.

## Module Ownership

Modules are business boundaries, not folders for visual tidiness. Current FKMed domains include:

- plan/family access;
- identity/access;
- audit;
- content/home;
- notifications;
- digital card;
- provider network;
- appointments;
- telemedicine;
- clinical documents;
- guides/tokens;
- finance;
- support;
- reimbursement;
- small kernels such as `error` and `upload`.

Cross-module rules are enforced by ArchUnit and Spring Modulith:

- call another module only through a public facade/API it exposes;
- do not depend on another module's repository, entity or internal implementation;
- react asynchronously through domain events when a module only needs to observe a fact;
- keep dependencies one-directional and acyclic.

## APIs

APIs are contracts. Every endpoint must have an explicit purpose, URL, method, request/response
shape, validation behavior, status codes, error codes, authentication/authorization expectation and
compatibility impact.

- Do not expose JPA entities.
- Pragmatic REST action endpoints are acceptable when they model product actions clearly, such as
  `POST /api/guides/{id}/token` or operator-sim transitions.
- JSON field names, enum values, nullability, dates and pagination envelopes are contract surface.
  Change them only with tests and OpenAPI snapshot updates.
- Dates/times use ISO-8601 over the wire; user formatting belongs to the frontend locale layer.

The committed OpenAPI snapshot (`docs/api/openapi.json`) is the public contract snapshot. Regenerate
it through the backend gate flag when controller contracts intentionally change; never edit the
snapshot by hand.

## Repositories

Each module owns its persistence. Command paths should go through the owning module's service and
repositories. Read models can use projections when they keep the code clearer, but must not become a
back door for cross-module writes.

Infrastructure adapters may use a module's persistence when they are implementing that module's own
technical concern. Other business modules must not touch that persistence directly.

## BFF, Gateway and Microservices

Do not introduce a BFF, gateway business layer or microservice split by default. Consider them only
for concrete pressure: different clients with divergent contracts, token isolation, legacy API
composition, independent deployment/scaling, team ownership or security/fault isolation.

The current architecture goal is a well-factored modular monolith. Microservices would distribute
bad modularity if the in-process boundaries are not already clean.

## Repo Layout

```txt
project-root
  backend/                 Java/Spring backend
  frontend/                Angular SPA
  infra/                   deployment-supporting infrastructure
  docs/
    specs/                 product specs
    adr/                   architecture decisions
    decision-log/          autonomous owner-authorized decisions
    architecture/          working rules by area
    api/openapi.json       committed contract snapshot
    architecture-diagrams/ modules diagram snapshot
  compose*.yaml
  README.md
  CLAUDE.md
  AGENTS.md
```

## Verified Map

The module diagram in `docs/architecture-diagrams/modules.puml` is generated from Spring Modulith.
`ModularityTest` compares the generated diagram to the committed snapshot during `verify`. If the
module map changes intentionally, regenerate the snapshot with the documented flag and review the
new dependencies as part of the PR.
