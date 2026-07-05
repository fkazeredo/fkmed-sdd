package com.fkmed.domain.identity;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a user tries to accept a legal-document version that is no longer the current one
 * (SPEC-0006 BR8): a newer version was published between display and acceptance, so the stale
 * acceptance is rejected and the client must re-fetch the current text. Mapped to HTTP 409 by
 * {@code infra.web.HttpErrorMapping}.
 */
public class LegalVersionOutdatedException extends DomainException {

  public static final String CODE = "legal.version-outdated";

  public LegalVersionOutdatedException() {
    super(CODE);
  }
}
