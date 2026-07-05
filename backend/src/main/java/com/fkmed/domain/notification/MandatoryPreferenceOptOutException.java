package com.fkmed.domain.notification;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a preference update tries to opt out of the e-mail channel of a mandatory event type
 * (SPEC-0004 BR7 — security/account types cannot be disabled; mapped to HTTP 422 by {@code
 * infra.web.HttpErrorMapping}).
 */
public class MandatoryPreferenceOptOutException extends DomainException {

  public static final String CODE = "notification.preference-mandatory";

  public MandatoryPreferenceOptOutException() {
    super(CODE);
  }
}
