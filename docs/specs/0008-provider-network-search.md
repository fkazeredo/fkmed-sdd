# 0008 - Provider Network Search

**Status:** Approved

## Goal

The beneficiary finds accredited providers through the funnel **locality → service type →
specialty → results** or by **searching the provider's name directly**, always within the
plan's coverage, with enough detail to reach care (address, phone, route).

## Scope

- Rede hub (4 cards: this search, Agendamento, Telemedicina, Minha Saúde).
- Step-by-step search assistant with selection screens (textual filter + alphabetical
  index) and session persistence of selections.
- Free-text search by provider name (owner-approved addition).
- Results list with reference date; provider detail with route/copy actions.

## Business Context

The accredited network is operator-loaded data (providers, specialties, seals). The plan's
coverage limits where the beneficiary can be served — the seeded plan is state-wide (RJ).
Provider "seals" are qualification badges whose official meaning is not yet defined by
product (parameterizable registry until then).

## Business Rules

- **BR1** — The assistant MUST enforce the order State → Municipality → Neighborhood:
  Municipality enables after State, Neighborhood after Municipality; changing State clears
  Municipality and Neighborhood; changing Municipality clears Neighborhood. "Buscar"
  enables with State + Municipality (Neighborhood optional, default "Todos").
- **BR2** — Selection screens MUST filter in real time, case- and accent-insensitive,
  keeping the alphabetical grouping; with no matches, show the empty state
  "Nenhum resultado para '{termo}'".
- **BR3** — Only localities with at least one **active** provider are offered (lists
  derived from the real network).
- **BR4** — The search MUST respect the plan's **coverage**: a state-wide plan (RJ) offers
  only its state; other UFs are not offered.
- **BR5** — Service types (fixed list, registry data): Consultórios–Clínicas–Terapias ·
  Exames Especiais · Hemodiálise · Hospitais para Internação · Laboratórios e Exames ·
  Pronto Atendimento – Horário Comercial · Pronto-Socorro 24h (Urgência e Emergência) ·
  TEA. Only **Consultórios–Clínicas–Terapias** has the specialty step; all other types
  skip straight to results.
- **BR6** — Specialties are registry data (≥ 15 seeded), listed alphabetically with search.
- **BR7** — Results MUST apply exactly the chosen filters, list only active providers, and
  display the **reference date of the query** (current date). Card: provider name,
  locality "BAIRRO, MUNICÍPIO – UF", seals when present.
- **BR8** — Name search: given **≥ 3 characters**, the system MUST search active providers
  by name (case/accent-insensitive, partial match) within the plan coverage, with an
  optional municipality filter; results use the same card format plus the service type.
- **BR9** — Neighborhood "Todos" returns providers of the whole municipality.
- **BR10** — No results: empty state "Não encontramos prestadores para esta busca" with
  actions "Alterar localidade" and "Alterar especialidade" (when applicable).
- **BR11** — Assistant selections MUST persist during the session: returning from results
  re-presents the chosen values; the locality summary on top of "O que deseja buscar?" is
  tappable for editing, preserving values.
- **BR12** — Provider detail MUST show name, service type, specialties, full address,
  clickable phone, seals with name and description on hover/touch, and the actions
  **Traçar rota** (opens the full address in the maps service, new tab) and **Copiar
  endereço** (with confirmation).
- **BR13** — An inactive provider MUST never appear in lists or detail; direct access to
  one answers "prestador indisponível".
- **BR14** — Seals are registry data (code, name, parameterizable description).

## Input/Output Examples

- `GET /api/network/municipalities?uf=RJ&q=rio` → `200` grouped: Cabo Frio, Rio Bonito,
  Rio Claro, Rio das Ostras, Rio de Janeiro, São José do Vale do Rio Preto, Três Rios.
- `GET /api/network/providers?uf=RJ&municipality=Rio de Janeiro&neighborhood=Centro&serviceType=CONSULTORIOS&specialty=CARDIOLOGIA`
  → `200 {"referenceDate":"2026-07-04","items":[…≥10 providers…]}`.
- `GET /api/network/providers/search?name=ca` → `422 {"code":"network.query-too-short"}`
  (error case).
- `GET /api/network/providers/{id}` of an inactive provider → `410
  {"code":"network.provider-unavailable"}` (error case).

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/network/states` · `/municipalities` · `/neighborhoods` | Funnel lists (derived, filtered by coverage) |
| GET | `/api/network/service-types` · `/specialties` | Registry lists |
| GET | `/api/network/providers` | Funnel results |
| GET | `/api/network/providers/search` | Name search (≥ 3 chars) |
| GET | `/api/network/providers/{id}` | Provider detail |

## Events

Not applicable.

## Persistence Changes

Migration V15 (Phase 3). **Geography registry `municipality`** (IBGE code PK, name, `uf` FK →
`uf_registry`) seeded with the **full official IBGE list** — the 27 UFs already live in
`uf_registry` (V11) + **~5,570 municipalities** (owner decision, DL-0014; source: IBGE
localidades API). Plan **coverage**: add `plan.coverage_uf` (nullable) — `coverage='ESTADUAL'`
→ the covered UF code; `'NACIONAL'` → `null` = all (DL-0014); the seeded plan is ESTADUAL/RJ.
Registries `service_type`, `specialty`, `seal`; `provider` (name ≤ 140, service_type_code,
`municipality` FK + free-text `neighborhood` + address fields — CEP, street, number,
complement —, phone, active); `provider_specialty`; `provider_seal`. **Provider seed** (RJ):
≥ 6 municipalities with active providers (Rio de Janeiro, Cabo Frio, Niterói, Três Rios, Rio
Bonito, Rio das Ostras), Rio neighborhoods including Centro, Copacabana and Tijuca, ≥ 15
specialties, ≥ 40 active providers with ≥ 10 Cardiologia in Rio de Janeiro/Centro, seals
varied, plus a few inactive providers for BR13 tests. The funnel lists (states/municipalities/
neighborhoods offered) are **derived from active providers within coverage** (BR3/BR4) over
this registry.

## Validation Rules

Name query ≥ 3 chars after trim. Funnel parameters must reference existing registry/derived
values. All list filtering server-side.

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Name query < 3 chars | `network.query-too-short` | 422 |
| Provider inactive/unknown | `network.provider-unavailable` | 410 |
| UF outside plan coverage | `network.outside-coverage` | 422 |

## Observability Requirements

Counter of searches by entry mode (funnel × name); log of zero-result searches (network
gap signal); no personal data involved.

## Tests Required

- **Domain/unit:** accent/case-insensitive matching; funnel-clearing rules; coverage filter.
- **Integration (Testcontainers):** derived locality lists; results filters; name search;
  inactive exclusion.
- **API contract:** all endpoints.
- **Frontend unit:** funnel enable/clear behavior; session persistence; empty states.
- **E2E:** CA journey RJ/Rio de Janeiro/Centro/Cardiologia ≥ 10 results; name search finds
  a seeded provider; route action opens maps URL.

## Acceptance Criteria

- **AC1** (BR2) — Given State RJ, when I type "rio" in the municipality selector, then I
  see only municipalities containing "rio", grouped by initial letter, per seed.
- **AC2** (BR7) — Given RJ / Rio de Janeiro / Centro + Consultórios–Clínicas–Terapias +
  Cardiologia, then I see ≥ 10 providers, all "CENTRO, RIO DE JANEIRO – RJ", under today's
  reference date.
- **AC3** (BR5) — Given service "Laboratórios e Exames", then the specialty step is
  skipped and results load directly.
- **AC4** (BR11) — Given I'm on results and tap "Pesquisar por localidade", then I return
  to step 1 with RJ/Rio de Janeiro/Centro pre-filled.
- **AC5** (BR10) — Given a neighborhood with no providers for the filter, then the empty
  state with adjustment actions appears (error case).
- **AC6** (BR12) — Given a provider detail, when I tap Traçar rota, then the maps service
  opens with the full address.
- **AC7** (BR1) — Given I change the State after choosing Municipality/Neighborhood, then
  those fields are cleared.
- **AC8** (BR8) — Given I search "cardio" by name, then active providers whose name matches
  appear regardless of neighborhood, within RJ coverage.

## Open Questions

- **OQ1** *(resolved — DL-0012)* — Seals are treated as **provider qualification badges** with
  registry-parameterizable descriptions until the product defines their official meaning.

## Out of Scope

Embedded map (route opens the external maps service); favorites; provider ratings; direct
scheduling from network results (scheduling is own-network only — SPEC-0009); ordering and
extra filters beyond BR7 (MAY, future).
