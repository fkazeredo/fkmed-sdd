package com.fkmed.domain.identity;

import com.fkmed.domain.error.DomainException;

/**
 * The password-reset link is expired, already used or unknown (SPEC-0002 BR10). Maps to HTTP 410
 * Gone — the link is no longer actionable; the user requests a fresh recovery.
 */
public final class ResetLinkInvalidException extends DomainException {

  public static final String CODE = "auth.reset-link-invalid";

  public ResetLinkInvalidException() {
    super(CODE);
  }
}
