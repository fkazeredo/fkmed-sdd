/**
 * The finance module (SPEC-0013 — Plano › Finanças): the contract's finances, exclusive to the
 * titular (BR1). Owns {@code invoice} (the monthly boletos with second copy + PIX copia-e-cola and
 * the antifraud validator) and {@code copay_entry} (the copay statement rows). The income-tax (IR)
 * statements and the annual debt-settlement declaration (Lei 12.007) are DERIVED from the invoices,
 * no table of their own.
 *
 * <p>Everything under {@code /api/finance/**} resolves the caller's beneficiary card from the JWT
 * and requires the caller to be the contract TITULAR (BR1) — the titular scope reuses {@code
 * domain.plan}'s {@link com.fkmed.domain.plan.BeneficiaryAccess}. Invoices and copay entries are
 * operator-originated (BR8): the portal is read-only over them; the only write path is the
 * operator-simulation ({@code application.sim}, SPEC-0018) through the {@link
 * com.fkmed.domain.finance.Invoices}/{@link com.fkmed.domain.finance.Copays} facades, which publish
 * {@link com.fkmed.domain.finance.InvoiceIssued} for the SPEC-0004 notification wiring. PDF
 * rendering reuses the OpenPDF setup of {@code domain.card} (ADR-0007).
 *
 * <p>Module map: ADR-0001 (Phase 5, the finance module).
 */
@org.springframework.modulith.ApplicationModule(displayName = "finance")
package com.fkmed.domain.finance;
