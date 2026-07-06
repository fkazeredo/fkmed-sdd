package com.fkmed.domain.finance;

/**
 * Derived lifecycle status of an invoice (SPEC-0013 BR2).
 *
 * <p>Enum keep-criterion (DECISIONS-BASELINE §0019): this is a technical/state classification
 * DERIVED at read time from the payment date and the due date (paid → {@link #PAID}; unpaid and due
 * date before today → {@link #OVERDUE}; otherwise {@link #OPEN}), never persisted and never runtime
 * reference data — new values cannot appear. It is not stored: {@link Invoice#status} computes it.
 */
public enum InvoiceStatus {
  OPEN,
  OVERDUE,
  PAID
}
