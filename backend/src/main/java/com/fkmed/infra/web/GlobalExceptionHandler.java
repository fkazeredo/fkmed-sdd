package com.fkmed.infra.web;

import com.fkmed.domain.error.DomainException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global API error handler (DECISIONS-BASELINE §0011): renders every {@link DomainException} with
 * the status registered in {@link HttpErrorMapping} and the pt-BR message resolved from the i18n
 * bundle. Unexpected errors become an opaque 500 — no internals leak to clients.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

  /** The product locale (single-locale product — docs/specs/README.md §UI norms). */
  static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");

  static final String INTERNAL_ERROR_CODE = "internal.error";

  private final MessageSource messageSource;

  @ExceptionHandler(DomainException.class)
  ResponseEntity<ApiErrorResponse> handleDomainException(DomainException exception) {
    String message =
        messageSource.getMessage(
            exception.getCode(), exception.getArgs(), exception.getCode(), PRODUCT_LOCALE);
    return ResponseEntity.status(HttpErrorMapping.statusOf(exception))
        .body(new ApiErrorResponse(exception.getCode(), message));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
    log.error("unexpected error handling request", exception);
    String message =
        messageSource.getMessage(INTERNAL_ERROR_CODE, null, INTERNAL_ERROR_CODE, PRODUCT_LOCALE);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiErrorResponse(INTERNAL_ERROR_CODE, message));
  }
}
