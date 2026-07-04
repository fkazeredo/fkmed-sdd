package com.fkmed.infra.web;

import com.fkmed.domain.error.DomainException;
import com.fkmed.domain.plan.PlanNotFoundException;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * The presentation-layer registry mapping each {@link DomainException} subclass to its HTTP status
 * (DECISIONS-BASELINE §0011). A build-time completeness test fails when a subclass lacks an
 * explicit entry; {@link #DEFAULT_STATUS} (422) is the documented fallback for exceptions
 * introduced mid-slice.
 */
public final class HttpErrorMapping {

  static final HttpStatus DEFAULT_STATUS = HttpStatus.UNPROCESSABLE_CONTENT;

  private static final Map<Class<? extends DomainException>, HttpStatus> MAPPINGS =
      Map.of(PlanNotFoundException.class, HttpStatus.NOT_FOUND);

  private HttpErrorMapping() {}

  /** The HTTP status registered for the exception's type, or 422 when unmapped. */
  public static HttpStatus statusOf(DomainException exception) {
    return MAPPINGS.getOrDefault(exception.getClass(), DEFAULT_STATUS);
  }

  /** The explicitly mapped exception types (consumed by the completeness gate). */
  public static Set<Class<? extends DomainException>> mappedTypes() {
    return MAPPINGS.keySet();
  }
}
