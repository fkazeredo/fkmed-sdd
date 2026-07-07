package com.fkmed.domain.finance;

import com.fkmed.domain.error.DomainException;

/**
 * The invoice is unknown or out of the caller's contract scope (SPEC-0013): 404 {@code
 * finance.invoice-not-found}, never revealing whether it exists.
 */
public class InvoiceNotFoundException extends DomainException {

  public static final String CODE = "finance.invoice-not-found";

  public InvoiceNotFoundException() {
    super(CODE);
  }
}
