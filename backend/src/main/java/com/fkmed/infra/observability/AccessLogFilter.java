package com.fkmed.infra.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Access log excluding health checks (SPEC-0001 §Observability). Never logs personal data — paths
 * in this product carry no identifiers (SPEC-0003 BR8).
 */
@Component
@Slf4j
public class AccessLogFilter extends OncePerRequestFilter {

  static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  static final String CORRELATION_ID_MDC_KEY = "correlationId";
  private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return "/api/system/health".equals(path) || path.startsWith("/actuator");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long start = System.nanoTime();
    String correlationId = correlationId(request);
    MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    response.setHeader(CORRELATION_ID_HEADER, correlationId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = (System.nanoTime() - start) / 1_000_000;
      log.info(
          "http correlationId={} method={} path={} status={} durationMs={}",
          correlationId,
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs);
      MDC.remove(CORRELATION_ID_MDC_KEY);
    }
  }

  private static String correlationId(HttpServletRequest request) {
    String header = request.getHeader(CORRELATION_ID_HEADER);
    if (header != null && SAFE_CORRELATION_ID.matcher(header).matches()) {
      return header;
    }
    return UUID.randomUUID().toString();
  }
}
