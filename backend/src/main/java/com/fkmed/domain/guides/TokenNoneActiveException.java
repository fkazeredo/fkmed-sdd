package com.fkmed.domain.guides;

import com.fkmed.domain.error.DomainException;

/**
 * There is no current, non-invalidated, non-expired attendance token for the given (already
 * scope-checked) beneficiary (SPEC-0012 §Error Behavior / BR10). The beneficiary-scope check itself
 * is a separate concern, enforced upstream via {@code
 * com.fkmed.domain.plan.BeneficiaryNotAccessibleException}.
 */
public class TokenNoneActiveException extends DomainException {

  public static final String CODE = "token.none-active";

  public TokenNoneActiveException() {
    super(CODE);
  }
}
