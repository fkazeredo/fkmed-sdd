# 0007 - Digital Card

**Status:** Draft

## Goal

The beneficiary's official identification in the plan: a visual card, the complementary
data sheet (CNS, ANS registration, coverage, additives) and a PDF download for offline use
at reception desks — for every beneficiary the user is allowed to access.

## Scope

- Digital card screen (visual card + data sheet) for the active beneficiary.
- "Salvar Carteirinha" → PDF download.
- "Copiar número" quick action.
- "Minhas Carteirinhas" list (one card per accessible beneficiary).

## Business Context

The card identifies the beneficiary at the provider's reception. CNS is sensitive: the
product masks it everywhere except on this screen and its PDF (SPEC-0003 BR8). Viewing a
dependent's card is an access to sensitive data and must be audited.

## Business Rules

- **BR1** — The screen MUST present the visual card (product brand, beneficiary full name,
  plan category and name, card number with icon, coverage seal) and the data sheet
  (additives list, CNS, ANS registration, coverage) of the **active beneficiary**, sourced
  from the operator registry.
- **BR2** — The coverage seal on the card MUST be identical to the coverage field of the
  data sheet (single source of truth).
- **BR3** — "Salvar Carteirinha" MUST generate a **PDF** containing at least: name, plan,
  card number, CNS, ANS registration, coverage and additives, with the card's visual
  identity.
- **BR4** — Switching the beneficiary in the selector MUST update card, sheet and the PDF
  generated afterwards.
- **BR5** — "Minhas Carteirinhas" MUST list exactly the beneficiaries accessible to the
  user (SPEC-0003 BR1); selecting one opens the card screen with that beneficiary active.
- **BR6** — "Copiar número" MUST copy exactly the 9 digits and confirm visually.
- **BR7** — Viewing a **dependent's** card by the titular MUST produce a sensitive-data
  audit entry (SPEC-0003 BR6).
- **BR8** — CNS is displayed in full **only** on this screen and in the PDF; everywhere
  else in the product it is masked.
- **BR9** — Formats in this POC: card number 9 digits, CNS 15 digits, ANS registration 6
  digits.
- **BR10** — An **inactive** beneficiary MUST NOT have a card displayed: show the state
  "carteirinha indisponível — contate os canais de atendimento".
- **BR11** *(MAY)* — Card back ("virar cartão") with service-channel phone numbers
  (SPEC-0014 content).

## Input/Output Examples

- `GET /api/cards/{beneficiaryId}` (MARIA active) → `200 {"fullName":"MARIA CLARA SOUZA
  LIMA","cardNumber":"001234567","cns":"700000000000001","ansRegistration":"326305",
  "coverage":"ESTADUAL","planName":"…","additives":["Urg/emerg Nacional Hr — Assistência"]}`.
- Same for an inactive beneficiary → `409 {"code":"card.unavailable"}` (error case).
- `GET /api/cards/{beneficiaryId}/pdf` → `200 application/pdf` (BR3 content).

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/cards/{beneficiaryId}` | Card + data sheet |
| GET | `/api/cards/{beneficiaryId}/pdf` | PDF download |

Both enforce SPEC-0003 scope (outside scope → `404 context.beneficiary-not-accessible`).

## Events

Not applicable (audit entry via SPEC-0003 contract).

## Persistence Changes

None beyond SPEC-0001 data (card reads the beneficiary/plan registry). If beneficiary
`active` flag is not yet present, add it in this slice's migration.

## Validation Rules

Read-only module; `beneficiaryId` scope-checked (SPEC-0003 BR3).

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Beneficiary inactive in the plan | `card.unavailable` | 409 |
| Beneficiary outside scope | `context.beneficiary-not-accessible` | 404 |

## Observability Requirements

Audit entry on dependent-card view (BR7); counter of PDF downloads. CNS never logged.

## Tests Required

- **Domain/unit:** masking boundaries (CNS exposure limited to this context).
- **Integration (Testcontainers):** card endpoint data; inactive state; scope denial;
  audit row on dependent view.
- **API contract:** both endpoints.
- **Frontend unit:** card/sheet rendering; copy action content; selector switch reload.
- **E2E:** MARIA views her card, downloads the PDF, switches to PEDRO and sees his number.

## Acceptance Criteria

- **AC1** (BR1, BR9) — Given MARIA active, then I see "MARIA CLARA SOUZA LIMA", card
  `001234567`, CNS `700000000000001`, ANS `326305`, coverage "Estadual" and the seeded
  additive.
- **AC2** (BR3) — Given "Salvar Carteirinha" clicked, then a PDF downloads containing all
  BR3 fields.
- **AC3** (BR4, BR7) — Given I select PEDRO in the selector, then the card shows
  `001234575` and the audit trail records the titular's access.
- **AC4** (BR5) — Given PEDRO authenticated with his own account, then Minhas Carteirinhas
  lists only his card.
- **AC5** (BR6) — Given "copiar número", then the clipboard contains exactly `001234567`
  and the confirmation appears.
- **AC6** (BR10) — Given an inactive beneficiary, then the unavailable state shows instead
  of the card (error case).

## Open Questions

- **OQ1** — PDF layout (portrait page vs card-sized) · pure presentation, cheap to change ·
  proposed default: card format laid on A4 with front visual + data block.

## Out of Scope

Dental card; QR-code validation (future version); provider-side validation of cards.
