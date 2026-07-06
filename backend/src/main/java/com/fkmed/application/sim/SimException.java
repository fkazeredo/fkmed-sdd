package com.fkmed.application.sim;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base of the operator-simulation API's client errors (SPEC-0018 §Error Behavior). Deliberately NOT
 * a {@code domain.error.DomainException}: the sim is an application-layer adapter, and the shared
 * {@code infra.web.HttpErrorMapping} must not depend on the application layer (ArchUnit §0010).
 * Each subclass carries its stable {@code code} (i18n key) and HTTP status; {@link
 * SimExceptionHandler} renders it through the same {@code ApiErrorResponse} shape as every other
 * API error.
 */
@Getter
public abstract class SimException extends RuntimeException {

  private final String code;
  private final HttpStatus status;

  protected SimException(String code, HttpStatus status) {
    super(code);
    this.code = code;
    this.status = status;
  }
}
