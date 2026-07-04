package com.fkmed.infra.web;

import com.fkmed.domain.audit.AuditContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Reads audit request-origin metadata (IP, User-Agent) from the current HTTP request (SPEC-0003
 * BR6), honouring {@code X-Forwarded-For} behind the reverse proxy. Returns {@link
 * AuditContext#none()} when there is no request in scope (e.g. a scheduled job).
 */
public final class HttpRequestMetadata {

  private HttpRequestMetadata() {}

  /** The current request's audit context, or {@link AuditContext#none()} outside a request. */
  public static AuditContext current() {
    if (RequestContextHolder.getRequestAttributes()
        instanceof ServletRequestAttributes attributes) {
      HttpServletRequest request = attributes.getRequest();
      return new AuditContext(clientIp(request), request.getHeader("User-Agent"));
    }
    return AuditContext.none();
  }

  private static String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
