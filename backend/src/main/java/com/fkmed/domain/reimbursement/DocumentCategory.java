package com.fkmed.domain.reimbursement;

/**
 * Fixed upload categories (SPEC-0015 BR8) — a structural, technical classification (never
 * operator-edited, no runtime growth), so per DECISIONS-BASELINE §0019 it is a plain enum rather
 * than a registry code (mirrors {@code domain.finance.InvoiceTab}).
 */
public enum DocumentCategory {
  /** Nota fiscal ou recibo — mandatory for every expense type. */
  RECEIPT,
  /** Pedido/relatório médico — mandatory for every type except Consulta (BR8/BR9). */
  MEDICAL_ORDER,
  /** Complementares — always optional, multiple allowed. */
  COMPLEMENTARY,
  /** Orcamento da previa analisada (SPEC-0017 BR3). */
  BUDGET
}
