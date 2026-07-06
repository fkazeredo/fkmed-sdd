package com.fkmed.application.sim;

import com.fkmed.infra.web.ApiErrorResponse;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders the operator-simulation API's client errors ({@link SimException}) through the shared
 * {@code ApiErrorResponse} envelope with the pt-BR message resolved from the i18n bundle — the same
 * contract as the global handler, but scoped to the sim so its exceptions need not live in the
 * domain kernel (they are an application concern; the infra global mapping must not depend on the
 * application layer — ArchUnit §0010). The more specific
 * {@code @ExceptionHandler(SimException.class)} wins over the infra fallback
 * {@code @ExceptionHandler(Exception.class)} across advices.
 */
@RestControllerAdvice(basePackages = "com.fkmed.application.sim")
@RequiredArgsConstructor
public class SimExceptionHandler {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");

  private final MessageSource messageSource;

  @ExceptionHandler(SimException.class)
  ResponseEntity<ApiErrorResponse> handleSim(SimException exception) {
    String message =
        messageSource.getMessage(exception.getCode(), null, exception.getCode(), PRODUCT_LOCALE);
    return ResponseEntity.status(exception.getStatus())
        .body(new ApiErrorResponse(exception.getCode(), message));
  }
}
