package com.fkmed.domain.plan;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when an uploaded photo exceeds the 5 MB limit (SPEC-0006 BR2). Mapped to HTTP 422 by
 * {@code infra.web.HttpErrorMapping}.
 */
public class PhotoTooLargeException extends DomainException {

  public static final String CODE = "profile.photo-too-large";

  public PhotoTooLargeException() {
    super(CODE);
  }
}
