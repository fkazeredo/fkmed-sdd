package com.fkmed.domain.reimbursement;

import com.fkmed.domain.error.DomainException;

/**
 * A provider identifier is invalid — name/council/number/UF/CPF-CNPJ/specialty (SPEC-0015 BR10):
 * 422 {@code reimbursement.provider-invalid}.
 */
public class ReimbursementProviderInvalidException extends DomainException {

  public static final String CODE = "reimbursement.provider-invalid";

  public ReimbursementProviderInvalidException() {
    super(CODE);
  }
}
