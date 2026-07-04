package com.fkmed.infra.web;

import com.fkmed.domain.error.DomainException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global API error handler (DECISIONS-BASELINE §0011): renders every {@link DomainException} with
 * the status registered in {@link HttpErrorMapping} and the pt-BR message resolved from the i18n
 * bundle. Framework exceptions that carry their own status ({@link ErrorResponse} — unknown route
 * 404, wrong method 405, unsupported media type 415…) keep it; access denials become 403. Only
 * genuinely unexpected errors become an opaque 500 — no internals leak to clients.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

  /** The product locale (single-locale product — docs/specs/README.md §UI norms). */
  static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");

  static final String INTERNAL_ERROR_CODE = "internal.error";
  static final String ACCESS_DENIED_CODE = "access.denied";

  private final MessageSource messageSource;

  @ExceptionHandler(DomainException.class)
  ResponseEntity<ApiErrorResponse> handleDomainException(DomainException exception) {
    String message =
        messageSource.getMessage(
            exception.getCode(), exception.getArgs(), exception.getCode(), PRODUCT_LOCALE);
    return ResponseEntity.status(HttpErrorMapping.statusOf(exception))
        .body(new ApiErrorResponse(exception.getCode(), message));
  }

  /** Authorization denial raised below the security filter chain (403, never 500). */
  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception) {
    String message =
        messageSource.getMessage(ACCESS_DENIED_CODE, null, ACCESS_DENIED_CODE, PRODUCT_LOCALE);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ApiErrorResponse(ACCESS_DENIED_CODE, message));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
    // Spring MVC exceptions carry their own status (404 unknown route, 405 wrong method…);
    // rendering them through the fallback would turn routine client errors into 500s
    // (regression: ApiErrorContractIT — review finding I1).
    if (exception instanceof ErrorResponse errorResponse) {
      HttpStatusCode status = errorResponse.getStatusCode();
      String message = reasonOf(status, errorResponse);
      log.debug("request error {}: {}", status.value(), message);
      return ResponseEntity.status(status)
          .body(new ApiErrorResponse("http." + status.value(), message));
    }
    log.error("unexpected error handling request", exception);
    String message =
        messageSource.getMessage(INTERNAL_ERROR_CODE, null, INTERNAL_ERROR_CODE, PRODUCT_LOCALE);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiErrorResponse(INTERNAL_ERROR_CODE, message));
  }

  private static String reasonOf(HttpStatusCode status, ErrorResponse errorResponse) {
    String detail = errorResponse.getBody().getDetail();
    if (detail != null && !detail.isBlank()) {
      return detail;
    }
    HttpStatus resolved = HttpStatus.resolve(status.value());
    return resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());
  }
}
