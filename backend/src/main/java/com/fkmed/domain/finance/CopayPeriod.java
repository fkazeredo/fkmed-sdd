package com.fkmed.domain.finance;

/**
 * The copay-statement period filter (SPEC-0013 BR5).
 *
 * <p>Enum keep-criterion (DECISIONS-BASELINE §0019): a closed, fixed set of filter shapes the API
 * offers — the calendar range each resolves to is computed by the controller against the product
 * clock. Not reference data (no runtime values, no label editing): a UI filter contract.
 */
public enum CopayPeriod {
  CURRENT_MONTH,
  LAST_3M,
  CUSTOM
}
