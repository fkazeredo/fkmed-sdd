# 0017 - Reimbursement Preview

**Status:** Draft

## Goal

Before (or after) paying an out-of-network provider, the beneficiary simulates how much
the plan would reimburse: **immediate** answer for consultations, **analyzed** answer (up
to 3 business days) for the other types — always non-binding and always carrying the
mandatory disclaimer.

## Scope

- Preview form (beneficiary + expense type) with the two response modes.
- Mandatory attachments for analyzed previews (budget + medical order/report).
- "Minhas prévias" list with situation and result.
- Mandatory disclaimer on every concluded preview.

## Business Context

The preview ("prévia de reembolso") is a market-standard expectation-management feature:
an estimate based on the plan's reimbursement table, never an authorization. In the POC,
the conclusion of analyzed previews is driven by the operator-simulation API (SPEC-0018),
emitting the same notification a real back office would.

## Business Rules

- **BR1** — The preview is available only under the SPEC-0015 BR1 eligibility gate.
- **BR2** — Form: beneficiary (SPEC-0003 scope rules) and expense type (SPEC-0015
  registry). For **Consulta**, the result MUST be **immediate**: estimated value from the
  current plan table.
- **BR3** — For **Exame / Terapia / Psicologia / Honorários**, the preview REQUIRES
  attachments — **orçamento** (budget, mandatory) and **pedido ou relatório médico**
  (mandatory) — under the SPEC-0015 BR8 upload limits; it is registered as "Em análise"
  and MUST conclude within **3 business days**, with a notification (SPEC-0004).
- **BR4** — Every concluded preview MUST display: estimated value, the base ("tabela do
  plano vigente") and the **mandatory disclaimer**, verbatim: "A prévia é uma estimativa
  com base nas informações e documentos enviados e nas regras do seu plano. Não representa
  autorização nem garantia de pagamento."
- **BR5** — A preview is **non-binding** and independent: it creates no reimbursement
  right, does not pre-fill nor gate SPEC-0015 requests.
- **BR6** — Each preview generates a unique protocol `PV-AAAAMMDD-####` (SPEC-0003 BR9).
- **BR7** — "Minhas prévias" lists the user-accessible previews with type, beneficiary,
  request date, situation (`EM_ANALISE` · `CONCLUIDA`) and, when concluded, the estimated
  value.

## Input/Output Examples

- `POST /api/reimbursement-previews` `{beneficiaryId, type:"CONSULTA"}` → `201
  {"protocol":"PV-20260704-0001","situation":"CONCLUIDA","estimatedValue":120.00,
  "base":"tabela do plano vigente","disclaimer":"A prévia é uma estimativa…"}`.
- `POST` type `EXAME` without budget attachment → `422
  {"code":"preview.attachments-required"}` (error case).
- `POST` type `EXAME` with both attachments → `201 {"situation":"EM_ANALISE"}`; conclusion
  (via SPEC-0018) → notification + result visible in the list.

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/reimbursement-previews` | Create preview (multipart when analyzed) |
| GET | `/api/reimbursement-previews` | "Minhas prévias" list |
| GET | `/api/reimbursement-previews/{id}` | Detail with result + disclaimer |

Conclusion of analyzed previews: SPEC-0018 endpoint.

## Events

`PreviewConcluded` (AFTER_COMMIT) → SPEC-0004 (estimated value + disclaimer).

## Persistence Changes

Migration (number at implementation): `reimbursement_preview` (id, protocol unique,
beneficiary_id, expense_type_code, situation `EM_ANALISE|CONCLUIDA`, estimated_value,
concluded_at, created_by, timestamps); `preview_document` (category `BUDGET|MEDICAL_ORDER`,
file ref, name, size). Reuses SPEC-0015 registries and the plan table.

## Validation Rules

Type in registry; attachments mandatory per BR3 with SPEC-0015 BR8 limits (content-checked,
≤ 2 MB/file, ≤ 20 MB total); `beneficiaryId` scope-checked.

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Missing mandatory attachments | `preview.attachments-required` | 422 |
| Attachment limits/content | *(SPEC-0015 document codes reused)* | 422 |
| Plan not eligible | `reimbursement.not-eligible` | 403 |
| Preview not found / out of scope | `preview.not-found` | 404 |

## Observability Requirements

Counters: previews per type, immediate × analyzed, average conclusion time. Business log
on creation/conclusion (protocol, type).

## Tests Required

- **Domain/unit:** immediate estimate from the table; mandatory-attachment matrix per type.
- **Integration (Testcontainers):** immediate consultation flow; analyzed flow with
  conclusion via the simulation API; notification emitted.
- **API contract:** all endpoints.
- **Frontend unit:** conditional attachment step; disclaimer always rendered on results.
- **E2E:** consultation preview shows R$ 120,00 + disclaimer; exam preview blocked without
  budget.

## Acceptance Criteria

- **AC1** (BR2, BR4) — Given a Consulta preview, then the immediate result shows
  R$ 120,00 with the verbatim disclaimer.
- **AC2** (BR3) — Given an Exame preview without the budget attached, then submission is
  blocked (error case).
- **AC3** (BR3, BR7) — Given an analyzed preview concluded (via SPEC-0018), then I am
  notified and "Minhas prévias" shows `CONCLUIDA` with the estimated value.
- **AC4** (BR6) — Given any created preview, then it has a unique `PV-…` protocol.
- **AC5** (BR5) — Given a concluded preview, then no reimbursement request exists for it
  (independence check).

## Open Questions

- **OQ1** — Do concluded previews expire (e.g. estimate valid for 30 days)? · affects copy
  and list badges · proposed default: **no expiry** — informational estimate only.

## Out of Scope

Binding pre-authorizations; converting a preview into a request automatically (possible
future); preview for expense types outside the SPEC-0015 registry.
