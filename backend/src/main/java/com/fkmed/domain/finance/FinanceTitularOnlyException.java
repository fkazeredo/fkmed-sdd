package com.fkmed.domain.finance;

import com.fkmed.domain.error.DomainException;

/**
 * The caller is not the contract titular (SPEC-0013 BR1). Every {@code /api/finance/**} route is
 * titular-only; a dependent (or an unresolvable card) is rejected with 403 {@code
 * finance.titular-only}.
 */
public class FinanceTitularOnlyException extends DomainException {

  public static final String CODE = "finance.titular-only";

  public FinanceTitularOnlyException() {
    super(CODE);
  }
}
