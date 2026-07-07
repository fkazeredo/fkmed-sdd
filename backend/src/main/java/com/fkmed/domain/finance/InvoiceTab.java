package com.fkmed.domain.finance;

/**
 * The invoice-list tab (SPEC-0013 BR2): {@link #OPEN} groups OPEN + OVERDUE invoices (due date
 * ascending, overdue first), {@link #PAID} lists paid invoices (competência descending).
 *
 * <p>Enum keep-criterion (DECISIONS-BASELINE §0019): a closed, fixed API filter contract (two
 * tabs), not runtime reference data.
 */
public enum InvoiceTab {
  OPEN,
  PAID
}
