package com.fkmed.domain.card;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when the target beneficiary is inactive in the plan (SPEC-0007 BR10): the card is not
 * displayed and the client shows the "carteirinha indisponível" state instead. Mapped to HTTP 409
 * by {@code infra.web.HttpErrorMapping} — distinct from {@link
 * com.fkmed.domain.plan.BeneficiaryNotAccessibleException} (404), which means the beneficiary is
 * outside the caller's family scope altogether.
 */
public class CardUnavailableException extends DomainException {

  public static final String CODE = "card.unavailable";

  public CardUnavailableException() {
    super(CODE);
  }
}
