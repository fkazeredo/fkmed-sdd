# 0012 - Guides and Tokens

**Status:** Approved

## Goal

Transparency over **authorization guides** (opened by providers with the operator) and
generation of the **attendance token** — a short-lived 6-digit code the beneficiary
presents at reception to validate care and prevent fraud.

## Scope

- Guides list (filters, on-demand refresh, orientative empty state) and guide detail.
- Status-change notifications.
- Token generation/display/copy, expiry and renewal.

## Business Context

Guides are created and moved by providers/operator — the beneficiary only follows them (in
the POC, transitions come from SPEC-0018). The token is an antifraud device: one valid
code per beneficiary at a time, short validity, presented verbally/visually at reception.

## Business Rules

- **BR1** — The screen MUST show header actions **Atualizar** and **Filtrar**, the plan
  strip, sections **Guias** and **Token**, and the beneficiary selector; the list shows
  the **active beneficiary's** guides and reloads on selector change.
- **BR2** — Guide list (most recent first): guide number, type (Consulta · SP/SADT–Exames ·
  Internação), requesting provider, request date and status badge. Filters: status and
  period.
- **BR3** — With no guides, the empty state MUST show: key icon, "Nenhuma solicitação em
  andamento", the orientation text and the button **"Atualizar informações"** — never a
  blank screen.
- **BR4** — Atualizar MUST fetch the latest guide state immediately, with a loading
  indicator.
- **BR5** — Guide detail MUST present the items (TUSS code, description, quantity, item
  status); when `AUTORIZADA`/`PARCIALMENTE_AUTORIZADA`, the **authorization password** and
  its validity; when `NEGADA` (or an item denied), the reason; plus contact guidance.
- **BR6** — Guide states: `EM_ANALISE → AUTORIZADA | PARCIALMENTE_AUTORIZADA | NEGADA |
  CANCELADA`; `AUTORIZADA/PARCIALMENTE_AUTORIZADA → EXECUTADA | CANCELADA`. Finals:
  `EXECUTADA`, `NEGADA`, `CANCELADA`. Item states: `EM_ANALISE`, `AUTORIZADO`, `NEGADO`.
  The guide status MUST derive from its items (all authorized = authorized; part =
  partially; all denied = denied). (State machines — enums.)
- **BR7** — A guide with an expired authorization password MUST show the notice
  "autorização expirada — procure o prestador/canais".
- **BR8** — Guide status changes MUST notify the user linked to the beneficiary (guide
  number, new status, denial reason when denied — never clinical details) — SPEC-0004.
- **BR9** — Gerar token MUST produce a **6-digit** code valid for **10 minutes** with a
  visible countdown; **one valid token per beneficiary at a time** — generating a new one
  invalidates the previous immediately.
- **BR10** — After expiry the code MUST NOT be displayed as valid; the screen shows "Token
  expirado" and offers generating a new one.
- **BR11** — Copiar MUST place exactly the 6 digits on the clipboard, with visual
  confirmation.
- **BR12** — The token belongs to the **beneficiary**: the titular MAY generate one for a
  dependent via the selector; authorship is audited (SPEC-0003 BR4).

## Input/Output Examples

- `GET /api/guides?beneficiaryId=…` (MARIA) → `200` 3 guides with distinct statuses (seed).
- `GET /api/guides/{id}` (authorized) → `200 {"status":"AUTORIZADA","authPassword":
  "AUT-482913","authValidUntil":"2026-08-03","items":[…]}`.
- `POST /api/tokens` `{beneficiaryId}` → `201 {"code":"483920","expiresAt":"…+10min"}`;
  second call → new code, previous invalidated.
- `GET /api/guides/{foreign-id}` → `404 {"code":"guide.not-found"}` (error case).

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/guides` | List with status/period filters |
| GET | `/api/guides/{id}` | Detail with items/password/reason |
| POST | `/api/tokens` | Generate token (invalidates previous) |
| GET | `/api/tokens/current` | Current valid token + expiry |

## Events

`GuideStatusChanged` (raised by SPEC-0018 transitions) → SPEC-0004 (BR8 content).

## Persistence Changes

Migration (number at implementation): `guide` (id, number unique, type, beneficiary_id,
requesting_provider, requested_at, status, auth_password, auth_valid_until, denial_reason);
`guide_item` (guide_id, tuss_code, description, quantity, status); `attendance_token`
(id, code, beneficiary_id, generated_at, expires_at, invalidated_at, created_by). Seed:
MARIA with 3 guides — em análise · autorizada (password `AUT-482913`, validity +30 days) ·
negada ("Documentação insuficiente"); PEDRO with none.

## Validation Rules

Guide type from fixed list. Token code exactly 6 digits, cryptographically random.
`beneficiaryId` scope-checked (SPEC-0003 BR3).

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Guide not found / out of scope | `guide.not-found` | 404 |
| No current valid token | `token.none-active` | 404 |

## Observability Requirements

Counter of tokens generated; business log on guide transitions (number + status, no
clinical data); audit of titular generating a dependent's token.

## Tests Required

- **Domain/unit:** guide-status derivation from items; token expiry/invalidation rules.
- **Integration (Testcontainers):** list/detail/filters; refresh; token single-validity;
  notification on transition.
- **API contract:** all endpoints.
- **Frontend unit:** countdown rendering; empty state; copy confirmation.
- **E2E:** MARIA sees 3 seeded guides; token generate → copy → regenerate invalidates.

## Acceptance Criteria

- **AC1** (BR2) — Given MARIA active (seed), then I see 3 guides with distinct badges (em
  análise, autorizada, negada).
- **AC2** (BR3) — Given PEDRO active (no guides), then I see the empty state with
  "Atualizar informações".
- **AC3** (BR5) — Given the authorized guide's detail, then I see password `AUT-482913`
  and its validity; on the denied guide, the reason "Documentação insuficiente".
- **AC4** (BR9, BR11) — Given I generate a token, then I see 6 digits with a 10:00
  countdown; Copiar places exactly the code on the clipboard.
- **AC5** (BR9) — Given an active token, when I generate another, then the previous stops
  being valid and only the new one is displayed.
- **AC6** (BR10) — Given an expired token, then the screen shows "Token expirado" and
  offers a new one (the old code never shows as valid) (error case).
- **AC7** (BR4, BR8) — Given the operator authorizes a guide under analysis (via
  SPEC-0018), then I receive the notification and the list reflects the new status after
  Atualizar.

## Open Questions

*(none — provider-side token validation was explicitly deferred)*

## Out of Scope

Guide creation/editing by the beneficiary; contesting denials through the portal (orient
to channels); provider reception interface for token validation.
