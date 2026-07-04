package com.fkmed.domain.identity;

import com.fkmed.domain.error.DomainException;

/**
 * The e-mail verification link is expired, already used or unknown (SPEC-0002 BR5). Maps to HTTP
 * 410 Gone — the link is no longer actionable; the user requests a fresh one (resend).
 */
public final class VerificationLinkInvalidException extends DomainException {

  public static final String CODE = "auth.verification-link-invalid";

  public VerificationLinkInvalidException() {
    super(CODE);
  }
}
