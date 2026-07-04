package com.fkmed.domain.audit;

/**
 * Request-origin metadata attached to an audit entry (SPEC-0003 BR6: IP + device identification).
 * Captured by the delivery/infra layer (it lives outside the domain) and passed into the recording
 * flow so the domain never reaches into the HTTP request.
 *
 * @param ip the caller's IP address, or {@code null} when unavailable (e.g. a scheduled job).
 * @param userAgent the caller's User-Agent, or {@code null} when unavailable.
 */
public record AuditContext(String ip, String userAgent) {

  private static final AuditContext NONE = new AuditContext(null, null);

  /** Metadata-less context for events without an HTTP origin (jobs, internal flows). */
  public static AuditContext none() {
    return NONE;
  }
}
