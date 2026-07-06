# ADR 0019: Module map revision — `domain.finance` (Plano › Finanças)

## Status

Proposed

## Context

SPEC-0013 introduces the contract's finances, **exclusive to the titular** (BR1): monthly invoices
(boletos) with 2nd copy + PIX copia-e-cola, an antifraud invoice validator, the copay statement,
income-tax (IR) statements and the annual debt-settlement declaration (Lei 12.007/2009). Invoices
and copay entries are **operator-originated** (BR8) — the portal is strictly read-only over them.
This is a new bounded context (contract finance custody + read + antifraud validation) that does not
belong to `domain.plan`, `domain.card` or any existing module. ADR-0001's "grow the map one spec at
a time" policy is triggered.

## Decision

Add **`domain.finance`** as the 13th verified Modulith module owning `invoice` (titular ref,
competência, due date, amount, **`digitable_line varchar(47)` with a `^[0-9]{47}$` check** — a
`char(47)`/bpchar broke the `String` mapping; the check preserves the 47-digit guarantee — `pix_code`,
`paid_at` nullable) and `copay_entry` (date, procedure, provider, beneficiary, amount). **IR and the
Lei 12.007 declaration are DERIVED** from invoices — no table. Public API is read-only under
`/api/finance/**`, **titular-only** (BR1): the caller's beneficiary card is resolved from the JWT and
must be the contract titular (reuse `domain.plan` `BeneficiaryAccess`) — a non-titular caller gets
`403 finance.titular-only`; the frontend additionally gates by the **active beneficiary** in the
selector (owner decision 2026-07-06 — Finanças is a titular-context area). Invoice status is
**derived** (open/overdue/paid); an overdue invoice shows the **valor atualizado** = original +
**multa 2%** + **juros de mora 1%/mês pro rata die** (BR2/OQ1, owner-decided). The antifraud
validator normalizes to exactly 47 digits and returns AUTHENTIC / NOT_RECOGNIZED (never suggesting
payment). PDFs (2nd-copy boleto with a "PAGO" watermark, IR, settlement declaration) reuse the
**OpenPDF** setup of `domain.card` (ADR-0007). The only write path is the **operator-simulation**
(SPEC-0018, ADR-0020) through the `Invoices`/`Copays` facades, which publish `InvoiceIssued` for the
SPEC-0004 notification wiring.

## Consequences

- **Positive:** a self-contained, read-only finance context; titular scope + PDF reuse proven
  patterns; IR/settlement derived (Rule Zero — no redundant tables); antifraud validator isolated
  and heavily unit-/mutation-tested (PIT 0 survivors on the money calculators).
- **Negative:** a 13th module raises the ModularityTest/diagram surface; the finance calculators
  (juros/multa, digitable-line, validator) are money-critical and demand mutation-grade tests.

## Alternatives Considered

- Fold finance into `domain.plan` — rejected: finance is a distinct bounded context (invoices,
  antifraud, fiscal documents) with its own lifecycle and titular-only surface.
- Persist IR/settlement — rejected (Rule Zero): both are pure aggregations of the invoices.

## Revision Triggers

- Online payment / renegotiation / due-date change (all out of scope now) would add write paths.
- Interest/penalty rules changing from the owner-fixed 2% + 1%/mo would revise `UpdatedAmount`.

## References

SPEC-0013 · SPEC-0018 (operator write path, ADR-0020) · SPEC-0004 (notifications) · SPEC-0003
(titular scope) · ADR-0001 (module map) · ADR-0007 (OpenPDF) · DECISIONS-BASELINE §0019 · migration
`V24__finance.sql` · diagram `docs/architecture-diagrams/modules.puml`.
