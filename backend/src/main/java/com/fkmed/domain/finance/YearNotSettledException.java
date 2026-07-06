package com.fkmed.domain.finance;

import com.fkmed.domain.error.DomainException;

/**
 * A settlement declaration (Lei 12.007) was requested for a base year that still has open or
 * overdue invoices (SPEC-0013 BR7): 409 {@code finance.year-not-settled}.
 */
public class YearNotSettledException extends DomainException {

  public static final String CODE = "finance.year-not-settled";

  public YearNotSettledException() {
    super(CODE);
  }
}
