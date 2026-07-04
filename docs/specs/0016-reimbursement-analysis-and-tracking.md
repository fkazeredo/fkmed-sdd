# 0016 - Reimbursement Analysis and Tracking

**Status:** Draft

## Goal

From submission to money in the account: the system analyzes the request automatically
(documentary completeness + table-based calculation with glosa), moves it through a strict
state machine with an immutable timeline, lets the beneficiary resolve documentation
pendencies and fix failed payments, and shows the statement of everything paid.

## Scope

- Reimbursement state machine and immutable timeline.
- Automatic analysis engine (completeness per type + value calculation).
- History with filters; detail with timeline and data blocks.
- Pendency resolution flow; bank-data correction flow after payment failure.
- Statement (extrato) of paid reimbursements.
- Seed of the 5 canonical historical requests.

## Business Context

The operator's back office analyzes and pays reimbursements. In this product the
**documentary check and the calculation are real engine code** (not simulated); the
remaining human/financial steps (approval confirmation, payment execution) are driven in
the POC by the operator-simulation API (SPEC-0018) — producing exactly the same events a
real back office would.

## Business Rules

- **BR1** — States and transitions (state machine — enum with documented keep criterion):

  ```
  EM_ANALISE → PROCESSAMENTO | PENDENTE_DOCUMENTACAO | NEGADO
  PENDENTE_DOCUMENTACAO → PROCESSAMENTO | CANCELADO (30 days without response)
  PROCESSAMENTO → APROVADO | PENDENTE_DOCUMENTACAO | NEGADO
  APROVADO → PAGO | PAGAMENTO_NAO_EFETUADO
  PAGAMENTO_NAO_EFETUADO → PAGO (after bank-data correction)
  Finals: PAGO · NEGADO · CANCELADO
  ```

  No other transition is representable.
- **BR2** — On submission the engine MUST automatically verify documentary completeness
  per expense type (SPEC-0015 BR9) and compute the value (BR3); the outcome leads to
  `PROCESSAMENTO`, `PENDENTE_DOCUMENTACAO` (with the pendency described) or `NEGADO`
  (with the reason). Non-conclusive cases remain in internal treatment — invisible to the
  beneficiary beyond the status.
- **BR3** — Calculation: reimbursed value = **min(amount paid, plan-table value for the
  type)** × plan multiple (1.0 in the POC) — per **session** for Terapia/Psicologia, per
  **event** otherwise. A shortfall generates a **partial glosa** with the reason "Valor
  excede a tabela do plano".
- **BR4** — Every status transition MUST create an immutable timeline event (date/time,
  status, description) and the corresponding notification (SPEC-0004). Timeline events are
  append-only.
- **BR5** — History: filters by beneficiary, status and period; card with protocol, type,
  beneficiary, request date, amount requested, amount reimbursed (when present) and status
  badge. Detail: vertical timeline always visible + blocks — expense · submitted documents
  · provider · **masked** bank data · values (requested, reimbursed, glosa + reason).
- **BR6** — In `PENDENTE_DOCUMENTACAO` the detail MUST show a banner describing the
  pendency and the action **"Enviar documentação"** (uploads within SPEC-0015 BR8 limits);
  resolving MUST attach the documents to the request, record the event, return the status
  to `PROCESSAMENTO` and recalculate the expected date.
- **BR7** — A pendency **pauses** the payment clock; resolution restarts it and the
  expected date is recalculated. A pendency without response for **30 days** moves the
  request to `CANCELADO` automatically, with notification (prior warning at 20 days —
  *MAY*).
- **BR8** — In `PAGAMENTO_NAO_EFETUADO` the detail MUST show the banner "Não foi possível
  creditar o valor" and the action **"Corrigir dados bancários"**, revalidating SPEC-0015
  BR11 (titular PF account only), recording the event and setting a new expected payment
  date.
- **BR9** — `NEGADO` and partial reimbursements MUST always show the denial/glosa reason.
- **BR10** — The statement considers only `PAGO` requests: filters by period and
  beneficiary; columns payment date, protocol, beneficiary, amount paid; footer with the
  period total, recalculated with the filters.
- **BR11** — Expected-date arithmetic uses **business days** (5/10 per SPEC-0015 BR12) and
  MUST expose the regulatory note (30 calendar days after complete documentation) in the
  success screen and the detail.

## Input/Output Examples

- `GET /api/reimbursements/{id}` (seed RE-20260601-0001) → `200 {"status":"PAGO",
  "amountRequested":150.00,"amountReimbursed":120.00,
  "glosa":{"amount":30.00,"reason":"Valor excede a tabela do plano"},
  "bank":{"account":"•••1234"},"timeline":[…]}`.
- `POST /api/reimbursements/{id}/pendency-documents` on a request not pending → `409
  {"code":"reimbursement.pendency-not-open"}` (error case).
- `GET /api/reimbursements/statement?from=2026-06-01&to=2026-06-30` → `200
  {"items":[{"protocol":"RE-20260601-0001","amountPaid":120.00,…}],"total":120.00}`.

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/reimbursements` | History with filters |
| GET | `/api/reimbursements/{id}` | Detail (timeline + blocks) |
| POST | `/api/reimbursements/{id}/pendency-documents` | Resolve pendency (multipart) |
| POST | `/api/reimbursements/{id}/bank-correction` | Fix bank data after failure |
| GET | `/api/reimbursements/statement` | Paid-only statement + total |

Approval/payment transitions are SPEC-0018 endpoints (POC back office).

## Events

`ReimbursementStatusChanged` (AFTER_COMMIT, one per transition) → SPEC-0004 with the
catalog: pendency (description + link), pendency resolved (in-app), approved (value +
glosa), paid (value, date, masked account), denied (reason + contact guidance), payment
not executed (correction guidance).

## Persistence Changes

Migration (number at implementation): add to SPEC-0015 tables — `reimbursement_timeline_event`
(request_id, occurred_at, status, description — append-only); pendency fields (description,
opened_at, deadline_at); payment fields (paid_at, failure fields); glosa fields (amount,
reason). Scheduled job for BR7 auto-cancellation. **Seed (canonical history):**

1. `RE-20260601-0001` · Consulta · MARIA · paid R$ 150,00 · **PAGO** R$ 120,00 · glosa
   R$ 30,00 ("Valor excede a tabela do plano").
2. `RE-20260615-0002` · Exame · PEDRO · R$ 300,00 · **PENDENTE_DOCUMENTACAO** ("Pedido
   médico ilegível — reenviar").
3. `RE-20260620-0003` · Terapia (4 sessões de R$ 100,00) · MARIA · **PROCESSAMENTO**.
4. `RE-20260410-0004` · Consulta · MARIA · R$ 200,00 · **NEGADO** ("Recibo sem
   identificação do profissional").
5. `RE-20260628-0005` · Honorários · MARIA · R$ 2.500,00 · **APROVADO** (R$ 900,00; glosa
   R$ 1.600,00), awaiting payment.

Each with a coherent timeline.

## Validation Rules

Pendency uploads: SPEC-0015 BR8 limits. Bank correction: SPEC-0015 BR11 rules. Transition
guards per BR1 (server-side, never trusting client state).

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Pendency action without open pendency | `reimbursement.pendency-not-open` | 409 |
| Bank correction outside `PAGAMENTO_NAO_EFETUADO` | `reimbursement.correction-not-allowed` | 409 |
| Invalid state transition (any origin) | `reimbursement.invalid-transition` | 409 |
| Request not found / out of scope | `reimbursement.not-found` | 404 |

## Observability Requirements

Business log per transition (protocol, from→to); metrics: requests per status, average
time to payment, pendency resolution time, auto-cancellations. Timeline is itself an
audit surface; bank data masked everywhere.

## Tests Required

- **Domain/unit:** state machine exhaustively (valid + invalid transitions); calculation
  engine (jqwik property tests over amounts/sessions — money-critical, PIT-scoped);
  business-day arithmetic; completeness rules per type.
- **Integration (Testcontainers):** submission → automatic outcome per scenario; pendency
  resolve; 30-day auto-cancel (clock manipulation); bank correction; statement totals.
- **API contract:** all endpoints and error codes.
- **Frontend unit:** timeline rendering; contextual banners per status; masked bank data.
- **E2E:** pendency resolution journey (seed item 2); denied reason visible (item 4);
  statement total for June (item 1).

## Acceptance Criteria

- **AC1** (BR2) — Given a submitted request with complete documentation, then it reaches
  `PROCESSAMENTO` with the analysis event in the timeline.
- **AC2** (BR6, BR7) — Given seed item 2 (`PENDENTE_DOCUMENTACAO`), when I send the
  requested document, then the status returns to `PROCESSAMENTO`, the timeline gains the
  event and the expected date is recalculated.
- **AC3** (BR8) — Given an item in `PAGAMENTO_NAO_EFETUADO` (driven via SPEC-0018), when I
  correct with valid titular bank data, then I get a new expected date and the event is
  recorded; with a PJ account, the correction is refused (error case).
- **AC4** (BR9) — Given seed item 4 (`NEGADO`), then the detail shows the reason "Recibo
  sem identificação do profissional".
- **AC5** (BR3, BR10) — Given seed item 1 (`PAGO`), then the detail shows requested
  R$ 150,00, reimbursed R$ 120,00 and glosa R$ 30,00 with reason; the June statement
  totals R$ 120,00.
- **AC6** (BR7) — Given a pendency without response for 30 days (time simulation), then
  the request becomes `CANCELADO` and the user is notified.
- **AC7** (BR1) — Given any invalid transition attempt (e.g. paying a `NEGADO` request via
  SPEC-0018), then it is rejected with `409` and no state changes (error case).

## Open Questions

- **OQ1** — The adhesion term (SPEC-0015 BR3) says audits can demand originals **even
  after approval**, but the BR1 state machine has no post-`PAGO` pendency transition —
  drafts conflict · affects the state model · proposed default: keep BR1 as-is in the POC
  (post-payment audits handled through channels, outside the system).

## Out of Scope

Appeal/contest flows (orient to channels); back-office internal work queues and manual
analysis screens (SPEC-0018 simulates outcomes); accounting/ERP integration; the request
wizard (SPEC-0015) and the preview (SPEC-0017).
