# 0013 - Plan Finance

**Status:** Draft

## Goal

The contract's finances in one area, exclusive to the titular: monthly invoices (boletos)
with second copy and **PIX copia-e-cola**, invoice authenticity validation (antifraud),
copay statement per usage, income-tax statements, and the **annual debt-settlement
declaration** (Lei 12.007).

## Scope

- Plano hub area (cards to this module's features + links to other modules).
- Invoice list/detail, digitable-line copy, PIX code copy, 2nd-copy PDF.
- Invoice validator (antifraud).
- Copay statement with filters and period total.
- Income-tax (IR) statements and annual debt-settlement declarations per base year.

## Business Context

Financial resources belong to the contract, therefore to the **titular** only. Invoices
and copay entries originate from the operator (seed in the POC, SPEC-0018 can generate);
the portal is read-only over them. Fraudulent boleto scams are common in Brazilian health
plans — the validator and its warning are a regulatory-grade UX requirement. Lei
12.007/2009 obliges operators to issue an annual debt-settlement declaration.

## Business Rules

- **BR1** — The whole module MUST be accessible only to the **titular**: financial cards
  are hidden from dependents and direct access is denied with a friendly screen.
- **BR2** — Invoice status MUST be derived: **paid** when a payment date exists; **overdue**
  when unpaid and due date before today; otherwise **open**. Tabs: **Em aberto** (ordered
  by due date ascending, overdue highlighted with the guidance "Atualize seu boleto pelos
  canais de atendimento") and **Pagos** (competência descending). Competência displayed as
  "Mês/AAAA".
- **BR3** — Invoice detail MUST offer: **Copiar linha digitável** (exactly the 47 digits,
  with confirmation), **PIX copia-e-cola** (copies the invoice's PIX code, with
  confirmation — code exposure only, no online payment) and **Baixar 2ª via (PDF)** with
  competência, due date, amount, digitable line and barcode; a paid invoice's PDF carries
  the "PAGO" watermark. The 2nd copy MUST be generatable for any invoice state.
- **BR4** — The validator MUST normalize the input (strip spaces/punctuation), require
  exactly **47 digits** after normalization (format error before any lookup), and compare
  against invoices issued for the operator's contracts:
  - **Autêntico** → confirm issuance and show competência, due date and amount.
  - **Não reconhecido** → prominent mandatory alert: "Boleto não reconhecido. **Não
    realize o pagamento** e procure os canais oficiais", linking to SPEC-0014. The
    validator MUST NEVER suggest paying an unrecognized invoice.
- **BR5** — Copay statement: filters by period (current month, last 3 months, custom
  range) and beneficiary (all of the family or individual); table with date, procedure,
  provider, beneficiary, copay amount; footer with the **total of the filtered period**;
  lines and total MUST recalculate on every filter change; empty state "Sem utilizações no
  período".
- **BR6** — IR statements MUST list only base years with payments; the PDF brings contract
  identification, the 12 monthly amounts (zeros where none) and the annual total.
- **BR7** — Annual debt-settlement declaration (Lei 12.007): base years in which **all**
  the year's invoices are paid MUST offer the declaration PDF (contract, beneficiaries,
  competências settled, issue date); years with open/overdue invoices MUST NOT offer it,
  showing guidance instead.
- **BR8** — Invoices and copay entries are operator-originated; the portal is strictly
  read-only over them.

## Input/Output Examples

- `GET /api/finance/invoices?tab=OPEN` (MARIA) → `200` current-month invoice + overdue one
  highlighted (seed).
- Dependent calling any `/api/finance/**` → `403 {"code":"finance.titular-only"}` (error
  case).
- `POST /api/finance/invoices/validate` `{"line":"23790.12345 67890.101112 ..."}` (a real
  seeded line with spaces) → `200 {"result":"AUTHENTIC","competencia":"Julho/2026",…}`;
  47 unknown digits → `200 {"result":"NOT_RECOGNIZED"}` with the mandatory warning; 30
  digits → `422 {"code":"finance.line-invalid-format"}` (error case).

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/finance/invoices` | Tabs open/paid (derived status) |
| GET | `/api/finance/invoices/{id}` · `/{id}/pdf` | Detail · 2nd copy PDF |
| POST | `/api/finance/invoices/validate` | Antifraud validator |
| GET | `/api/finance/copay` | Statement with filters + total |
| GET | `/api/finance/tax-statements` · `/{year}/pdf` | IR years · PDF |
| GET | `/api/finance/settlement-declarations` · `/{year}/pdf` | Lei 12.007 years · PDF |

All titular-only (BR1).

## Events

`InvoiceIssued` (new competência — raised by seed/SPEC-0018) → SPEC-0004 (competência,
amount, due date, link). Due-in-3-days reminder *(MAY, future)*.

## Persistence Changes

Migration (number at implementation): `invoice` (id, contract/titular ref, competencia,
due_date, amount, digitable_line char(47) unique, pix_code, paid_at nullable);
`copay_entry` (id, date, procedure, provider, beneficiary_id, amount). IR and settlement
data derived from invoices. Seed: 4 invoices (1 open in current month, 1 overdue, 2 paid)
with realistic 47-digit lines + PIX codes, and 8 copay entries within the last 90 days
across MARIA and PEDRO.

## Validation Rules

Validator input: normalize then exactly 47 digits. Period filters: valid ranges. All
financial endpoints scope-checked to the titular of the contract.

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Dependent accessing module | `finance.titular-only` | 403 |
| Validator input ≠ 47 digits | `finance.line-invalid-format` | 422 |
| Invoice not found | `finance.invoice-not-found` | 404 |
| Declaration year not settled | `finance.year-not-settled` | 409 |

## Observability Requirements

Counter of validator uses split by result (authentic × not recognized — fraud signal);
PDF downloads per type. Digitable lines and PIX codes never logged in full.

## Tests Required

- **Domain/unit:** status derivation; validator normalization/format; settlement-year
  eligibility; IR aggregation (12 months, zeros).
- **Integration (Testcontainers):** tabs/ordering; validator against seeded lines;
  copay filters + total; titular-only enforcement.
- **API contract:** all endpoints and error codes.
- **Frontend unit:** copy actions; watermark presence flag; filter recalculation; friendly
  denial screen.
- **E2E:** titular sees seeded invoices; validator authentic + not-recognized journeys;
  dependent denied.

## Acceptance Criteria

- **AC1** (BR2) — Given the seeded contract, then "Em aberto" shows the current-month
  invoice and the overdue one (highlighted), and "Pagos" shows the 2 paid ones.
- **AC2** (BR3) — Given "Copiar linha digitável" on the detail, then the clipboard holds
  the 47 digits and I see the confirmation; same for the PIX code.
- **AC3** (BR4) — Given I paste a seeded invoice's line (with spaces) into the validator,
  then I see "Boleto autêntico" with competência/amount; given 47 unknown digits, I see
  the not-recognized alert with the do-not-pay guidance (error case).
- **AC4** (BR5) — Given the statement filtered to last month and beneficiary PEDRO, then
  table and total reflect only those entries.
- **AC5** (BR6) — Given a base year with payments, when I download the IR statement, then
  the PDF shows the 12 months and the annual total.
- **AC6** (BR1) — Given a dependent authenticated, then no financial card appears and
  direct access to the screens is denied (error case).
- **AC7** (BR3) — Given a paid invoice, then its downloaded 2nd copy shows the "PAGO"
  watermark.
- **AC8** (BR7) — Given a base year fully paid, then its settlement declaration PDF is
  offered; given the current year with an open invoice, it is not (error case).

## Open Questions

- **OQ1** — Interest/penalty amounts on overdue invoices · shown or channel-only ·
  proposed default: **not shown** (guidance to channels), per the reference product.

## Out of Scope

Online payment (card/PIX checkout); debt renegotiation; due-date change; invoice
generation rules (operator's back office — POC seeds/simulates via SPEC-0018).
