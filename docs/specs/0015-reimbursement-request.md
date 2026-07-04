# 0015 - Reimbursement Request

**Status:** Draft

## Goal

A beneficiary paid an out-of-network provider (free choice) and gets reimbursed through a
100% digital journey: eligibility check, adhesion term, expense and documents, provider
and bank data, review and submission with protocol and expected payment date — under the
ANS regulatory deadlines.

## Scope

- Reimbursement hub (request · preview → SPEC-0017 · history/statement → SPEC-0016) with
  the plan-eligibility gate.
- The request wizard (steps 0–6) with per-step validation.
- Versioned adhesion-term acceptance bound to each request.
- Document uploads with per-file/total limits and a live meter.
- Submission: protocol, expected date, initial state and idempotency.

## Business Context

Reimbursement (livre escolha) refunds expenses paid to non-network providers, limited to
the plan's reimbursement table. ANS grants beneficiaries **12 months** from the care date
to request it, and caps payment at **30 calendar days** after complete documentation. The
analysis itself is SPEC-0016; this spec owns getting a valid, complete request into the
system.

## Business Rules

- **BR1** — The hub MUST validate plan eligibility before any flow: a plan without
  reimbursement right shows the informative screen "Seu plano não possui reembolso" with
  guidance, and **no module route proceeds**.
- **BR2** — Step 0 (contacts): display the requester's contact e-mail and mobile with the
  notice "manteremos contato por estes canais durante a análise" and the action "Atualizar
  dados" (opens SPEC-0006, returns to the wizard). Both contacts are **mandatory** to open
  a request (SPEC-0006 BR6).
- **BR3** — Step 1 (term): current adhesion term, scrollable, with mandatory acceptance —
  including the declaration of **keeping original documents for 5 years**, subject to
  audit. The acceptance MUST be recorded (version + timestamp) and bound to the request;
  without it there is no advance.
- **BR4** — Step 2 (expense): attended beneficiary (pre-filled with the active one;
  changeable by the titular within their scope; fixed to self for a dependent requester);
  expense type from the registry — Consulta médica · Exame · Terapia (sessões) ·
  Psicologia (sessões) · Honorários médicos (cirurgia) · Outros; care date; amount paid.
- **BR5** — Deadline (ANS): the care date MUST NOT be in the future and MUST be within
  **12 months** of the request date; violation blocks step 2 with "Prazo para solicitação
  expirado".
- **BR6** — Amount paid MUST be greater than zero (pt-BR currency mask).
- **BR7** — Terapia/Psicologia: number of sessions ≥ 1, each with a valid date and amount
  > 0; session dates MUST NOT be future nor beyond the BR5 limit; the **sum of session
  amounts MUST equal the informed total**.
- **BR8** — Step 3 (documents): uploads by category — **Nota fiscal ou recibo**
  (mandatory) · **Pedido/relatório médico** (mandatory for Exame, Terapia, Psicologia and
  Honorários) · **Complementares** (optional, multiple). Formats JPG/PNG/PDF validated by
  real content; **≤ 2 MB per file** and **≤ 20 MB total**, with a live remaining-space
  meter reflecting additions and removals; exceeding a limit blocks that attachment with a
  specific message. Each attachment lists name and size, with removal.
- **BR9** — The "Veja a documentação necessária" summary MUST vary by expense type, with
  at least: *Consulta* — invoice/receipt with patient name, service description, date,
  amount and professional identification (name, council + number, CPF/CNPJ, address,
  signature/stamp); *Exame* — the above **+ medical order**; *Terapia/Psicologia* —
  receipt with **per-session dates and amounts** + order or report from the requesting
  professional; *Honorários (cirurgia)* — invoice/receipt per professional + **medical
  report** of the procedure (+ anesthesia bulletin as complementary when applicable);
  *Outros* — invoice/receipt + report justifying the expense.
- **BR10** — Step 4 (provider): professional/establishment name; professional council from
  the registry (CRM · CRP · CRO · CREFITO · Outro) + number + UF; valid CPF or CNPJ;
  specialty.
- **BR11** — Step 5 (bank data): bank (registry list), agency, account + digit, type
  Corrente/Poupança, under the permanent notice: only a **personal (PF) account of the
  plan titular** — third-party, salary and PJ accounts MUST be refused at this step. For a
  dependent requester, the data is the titular's (titular shown as payee).
- **BR12** — Step 6 (review): full summary with per-step editing; "Enviar solicitação" →
  success screen with protocol `RE-AAAAMMDD-####` (SPEC-0003 BR9), **expected payment
  date** — **5 business days** (Consulta) or **10 business days** (other types) after
  submission with complete documentation, parameterizable — plus the note about the
  regulatory ceiling of **30 calendar days** after complete documentation, and the action
  "Acompanhar solicitação".
- **BR13** — Submission MUST create the request as `EM_ANALISE`, record the first
  immutable timeline event and notify (SPEC-0004). Accidental resubmission of the same
  form (double click/retry) MUST NOT create a duplicate.
- **BR14** — Each step validates before advancing, with messages next to the fields; the
  final submission revalidates everything server-side.
- **BR15** — Authorship and target follow SPEC-0003: titular for self and dependents,
  dependent only for self; always audited.

## Input/Output Examples

- `POST /api/reimbursements` (complete consultation of R$ 150,00, idempotency key K) →
  `201 {"protocol":"RE-20260704-0001","status":"EM_ANALISE",
  "expectedPaymentDate":"2026-07-13"}`; immediate retry with key K → `201` same protocol.
- Care date 14 months ago → `422 {"code":"reimbursement.deadline-expired"}` (error case).
- 4 sessions summing R$ 380,00 with total R$ 400,00 → `422
  {"code":"reimbursement.sessions-sum-mismatch"}` (error case).
- 3 MB file → `422 {"code":"reimbursement.document-too-large"}` (error case).
- PJ account (CNPJ payee) → `422 {"code":"reimbursement.bank-account-not-allowed"}` (error
  case).

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/reimbursements/eligibility` | Plan gate (BR1) |
| GET | `/api/reimbursements/term` | Current adhesion term (version + text) |
| GET | `/api/reimbursements/documentation-guide?type=…` | BR9 summary |
| POST | `/api/reimbursements` | Submit request (multipart; Idempotency-Key header) |
| GET | `/api/reimbursements/catalog` | Expense types, councils, banks (registries) |

## Events

`ReimbursementSubmitted` (AFTER_COMMIT) → SPEC-0004 (protocol, type, amount, expected
date) and SPEC-0016 (starts the automatic analysis).

## Persistence Changes

Migration (number at implementation): registries `expense_type`, `professional_council`,
`bank`; `reimbursement_table` (expense_type_code, amount, per_session flag, multiple) —
seed: Consulta 120,00 · Exame 80,00 · Terapia/Psicologia 60,00 per session · Honorários
900,00 · multiple 1.0; `reimbursement_adhesion_term` (version, published_at, body) + seed;
`reimbursement_request` (id, protocol unique, beneficiary_id, expense_type_code, care_date,
amount, provider fields, bank fields, term version, status, expected_payment_date,
idempotency_key unique, created_by, timestamps); `reimbursement_session_item` (date,
amount); `reimbursement_document` (category, file ref, name, size).

## Validation Rules

Care date: not future, within 12 months. Amount > 0. Sessions: ≥ 1, each valid, sum =
total. Documents: JPG/PNG/PDF content-checked, ≤ 2 MB each, ≤ 20 MB total, mandatory
categories per type. Provider: name ≤ 140; council in registry + number ≤ 10 digits + UF;
CPF (11) or CNPJ (14) with valid check digits; specialty required. Bank: registry bank;
agency 4 digits; account + digit; type in {Corrente, Poupança}; payee = plan titular (PF).

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Plan without reimbursement | `reimbursement.not-eligible` | 403 |
| Contacts missing | `reimbursement.contacts-missing` | 409 |
| Term not accepted / stale version | `reimbursement.term-not-accepted` | 422 |
| Care date beyond 12 months / future | `reimbursement.deadline-expired` / `reimbursement.care-date-invalid` | 422 |
| Amount ≤ 0 | `reimbursement.amount-invalid` | 422 |
| Sessions sum ≠ total | `reimbursement.sessions-sum-mismatch` | 422 |
| File > 2 MB · total > 20 MB · bad content | `reimbursement.document-too-large` / `reimbursement.total-size-exceeded` / `reimbursement.document-invalid-content` | 422 |
| Mandatory document category missing | `reimbursement.document-required` | 422 |
| Provider identifiers invalid | `reimbursement.provider-invalid` | 422 |
| Non-titular / PJ / salary account | `reimbursement.bank-account-not-allowed` | 422 |

## Observability Requirements

Business log on submission (protocol, type, amount — beneficiary masked); counters:
requests per type, blocked-by-deadline, upload rejections per reason. Bank data never
logged.

## Tests Required

- **Domain/unit:** deadline boundary (exactly 12 months); sessions sum (jqwik property);
  money arithmetic; idempotency behavior.
- **Integration (Testcontainers):** full submission; each 422 path; multipart limits;
  duplicate-submit protection; term binding.
- **API contract:** all endpoints and error codes.
- **Frontend unit:** step gating; meter arithmetic; per-field messages; payee display for
  dependent requester.
- **E2E:** complete consultation request journey to the success screen (protocol shown).

## Acceptance Criteria

- **AC1** (BR3) — Given the term not accepted, then step 1 does not advance; accepted, the
  record stores version and timestamp bound to the request.
- **AC2** (BR8) — Given a 3 MB file, then it is refused with "Arquivo excede 2 MB"; given
  a set crossing 20 MB, the new attachment is refused and the meter shows the limit
  (error case).
- **AC3** (BR5) — Given a care date 14 months ago, then step 2 blocks with "Prazo para
  solicitação expirado" (error case).
- **AC4** (BR7) — Given 4 sessions summing R$ 380,00 for a total of R$ 400,00, then
  advancing is blocked for value inconsistency (error case).
- **AC5** (BR12, BR13) — Given the complete wizard for a R$ 150,00 consultation, when I
  submit, then I receive the `RE-…` protocol, expected date, status `EM_ANALISE`, the
  initial timeline event and the notifications (e-mail + in-app).
- **AC6** (BR13) — Given a double click on "Enviar solicitação", then exactly one request
  is created.
- **AC7** (BR4, BR11, BR15) — Given PEDRO (dependent) requesting, then the beneficiary is
  himself (no switching) and the displayed bank payee is the titular MARIA.
- **AC8** (BR1) — Given a user whose plan has no reimbursement (test scenario), then the
  hub shows the informative screen and no module route works (error case).

## Open Questions

- **OQ1** — Multiple expenses under a single protocol ("adicionar outra despesa" before
  submitting) · changes data model · proposed default: **not in the POC** (version 2).
- **OQ2** — Reimbursement multiple per plan category (premium plans reimburse more) ·
  affects the calculation parameterization · proposed default: POC uses **1.0**, kept as a
  plan parameter.

## Out of Scope

Dental/medication reimbursement; physical document submission; appeal/contest of denials
through the portal (orient to channels); payment to third-party accounts; the analysis and
payment lifecycle (SPEC-0016); the preview (SPEC-0017).
