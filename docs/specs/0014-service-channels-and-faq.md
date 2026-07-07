# 0014 - Service Channels and FAQ

**Status:** Approved

## Goal

Every contact channel with the operator in one place, antifraud guidance, the Brazilian
Sign Language (Libras) service request, and a searchable FAQ — reducing support friction
and fraud exposure.

## Scope

- Channels page with actionable contacts.
- Antifraud section with a deep-linkable anchor (destination of the Home banner).
- Central de Libras: explanation + service request registration.
- FAQ with categories, real-time search and accordion.

## Business Context

Channel data, antifraud copy and FAQ entries are operator-managed content (seed migration
in the POC) — the single source of truth for what the portal displays. The FAQ must teach
the reimbursement rules the portal enforces (SPEC-0015..0017), keeping support and product
consistent.

## Business Rules

- **BR1** — Channel cards: **Central de Atendimento 24h** (capitals + other localities
  numbers), **WhatsApp oficial** (opens the official-number chat in a new tab),
  **Ouvidoria** (phone + service hours) and **ANS** (informative agency number). Phones
  MUST be tap-to-call on capable devices AND written in full for desktop use.
- **BR2** — Displayed channels MUST come exclusively from the operator-managed content
  registry (no hardcoded contact anywhere in the product).
- **BR3** — The antifraud section MUST be reachable by direct anchor link (destination of
  the Home fraud banner — SPEC-0005 BR9), with the title "Alerta de golpe!", the message
  "A operadora não solicita dados ou pagamentos por WhatsApp" and best practices: never
  share password or token; validate invoices before paying (link to the SPEC-0013
  validator); use only the official channels of this page.
- **BR4** — Central de Libras: explanatory page with operating hours and the button
  **"Solicitar atendimento em Libras"** — it MUST register the request (beneficiary,
  date/time, situation `REGISTERED|ATTENDED`) and confirm: within hours, "nossa equipe
  iniciará a videochamada em instantes"; outside hours, show the hours and offer
  registering for the next period.
- **BR5** — FAQ: search field + category filters (Reembolso · Carteirinha · Agendamento ·
  Telemedicina · Boletos · Rede) + accordion where opening a question closes the previous
  one. The search MUST filter in real time over title AND content, case/accent-insensitive;
  a selected category restricts the set; "Todas" restores; no matches → empty state
  "Nenhum resultado para '{termo}'".
- **BR6** — Seed content MUST contain ≥ 12 questions across the 6 categories, including
  ≥ 3 Reembolso entries aligned with the reimbursement rules (12-month deadline,
  documentation per expense type, non-binding preview).

## Input/Output Examples

- `GET /api/support/channels` → `200` ordered channel list (BR1 types).
- `GET /api/support/faq?q=reembolso` → `200` only reimbursement-related questions.
- `POST /api/support/libras-requests` `{beneficiaryId}` within hours → `201
  {"situation":"REGISTERED","nextStep":"videocall-shortly"}`; outside hours → `201` with
  `"nextStep":"next-period"` and the hours in the response.

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/support/channels` | Channel cards content |
| GET | `/api/support/antifraud` | Antifraud section content |
| GET | `/api/support/faq` | Questions (category + q filters) |
| POST | `/api/support/libras-requests` | Register Libras service request |

## Events

Not applicable (Libras request is recorded and audited; the videocall is conducted by the
operator's team outside the POC).

## Persistence Changes

Migration `V25__support_channels_and_faq.sql`: `support_channel` (type
`CENTRAL|WHATSAPP|OUVIDORIA|ANS` — a `*Codes` constants holder per DECISIONS-BASELINE §0019, not a
registry table: the 4 types are fixed by the product's contact surface —, label, optional sublabel
for a type with more than one row, e.g. Central 24h's "Capitais"/"Demais localidades", value
phone/URL, optional hours, display_order); `support_antifraud` (a single content row — title,
message — DL-0023: the antifraud section's 3 best-practice bullets and the validator link are
static frontend copy, not persisted); `faq_entry` (category — a `*Codes` constants holder, same
§0019 criterion, 6 fixed categories —, question ≤ 200, answer, display_order, active);
`libras_request` (beneficiary_id, requested_at, situation — `REGISTERED|ATTENDED`, a genuine enum:
a lifecycle state machine per §0019). Seeds per BR1/BR6 with fictitious placeholder numbers/hours
(OQ1 — DL-0024).

## Validation Rules

FAQ search: server- or client-side filtering MUST normalize case/accents. Libras request:
`beneficiaryId` scope-checked.

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| *(module is content-serving; standard auth/scope errors only)* | — | — |

## Observability Requirements

Counter of Libras requests and zero-result FAQ searches (content-gap signal). Audit entry
for the Libras request (beneficiary, timestamp).

## Tests Required

- **Domain/unit:** accent/case-insensitive FAQ matching; hours window for Libras response.
- **Integration (Testcontainers):** content endpoints; Libras request persisted + audited.
- **API contract:** all endpoints.
- **Frontend unit:** anchor positioning; accordion single-open; search + category combo;
  tap-to-call/WhatsApp links.
- **E2E:** Home fraud banner lands on the antifraud anchor; FAQ search journey; Libras
  request confirmation.

## Acceptance Criteria

- **AC1** (BR5) — Given I search "reembolso" in the FAQ, then only related questions
  appear; clearing the search restores the full list.
- **AC2** (BR3) — Given I click "Saiba mais" on the Home fraud banner, then this page
  opens positioned at the antifraud section.
- **AC3** (BR5) — Given I open a question with another one open, then the previous closes.
- **AC4** (BR4) — Given "Solicitar atendimento em Libras" within hours, then the request
  is registered and I see the confirmation with the next step; outside hours, I see the
  operating hours (error-path case).
- **AC5** (BR1) — Given the WhatsApp card, then the chat opens with the official number in
  a new tab.

## Open Questions

- **OQ1** — *(resolved, DL-0024)* Definitive channel numbers and service hours · owner-decided:
  fictitious placeholders in the V25 seed, swapped by a future content migration when the owner
  provides the real ones. Does not block the slice.

## Out of Scope

Live chat with attendants; ombudsman protocol opening through the portal (channel informs
phone/hours); conducting the Libras videocall (operator's team).
