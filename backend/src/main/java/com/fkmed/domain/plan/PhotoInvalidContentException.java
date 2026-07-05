package com.fkmed.domain.plan;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when an uploaded photo's real content is not JPG or PNG (SPEC-0006 BR2). The check is on
 * the file's magic bytes, never its extension, so an executable renamed {@code .png} is refused.
 * Mapped to HTTP 422 by {@code infra.web.HttpErrorMapping}.
 */
public class PhotoInvalidContentException extends DomainException {

  public static final String CODE = "profile.photo-invalid-content";

  public PhotoInvalidContentException() {
    super(CODE);
  }
}
